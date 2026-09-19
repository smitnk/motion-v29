package com.smitnk.motioncanvas.video

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.audio.DefaultGainProvider
import androidx.media3.common.audio.GainProcessor
import androidx.media3.common.audio.ToInt16PcmAudioProcessor
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.smitnk.motioncanvas.audio.AudioClip
import com.smitnk.motioncanvas.audio.AudioTrack
import java.io.File

/**
 * Multi-track audio/video export using AndroidX Media3 Transformer.
 *
 * Media3 Composition mixes overlapping sequences into a single output audio track.
 * Each MotionCanvas clip becomes its own audio sequence, which lets clips overlap
 * even when they belong to the same logical track. Gaps provide timeline offsets.
 */
@UnstableApi
object Media3MultiTrackExporter {
    fun export(
        context: Context,
        video: File,
        tracks: List<AudioTrack>,
        fps: Int,
        output: File,
        onComplete: (Result<Unit>) -> Unit
    ) {
        require(video.exists()) { "Video file does not exist" }
        val activeClips = tracks.filter { it.enabled }
            .flatMap { it.clips.filter { clip -> !clip.muted && File(clip.uri).exists() } }
        if (activeClips.isEmpty()) {
            video.copyTo(output, overwrite = true)
            onComplete(Result.success(Unit))
            return
        }

        val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(video)))
            .setRemoveAudio(true)
            .build()
        val sequences = mutableListOf<EditedMediaItemSequence>()
        sequences += EditedMediaItemSequence.withVideoFrom(listOf(videoItem))

        activeClips.forEach { clip ->
            val startUs = clip.startFrame.coerceAtLeast(0).toLong() * 1_000_000L / fps.coerceIn(1, 60)
            val clipStartUs = clip.inFrame.coerceAtLeast(0).toLong() * 1_000_000L / fps.coerceIn(1, 60)
            val effectiveOutFrame = if (clip.outFrame == Int.MAX_VALUE) {
                clip.inFrame + clip.durationFrames.coerceAtLeast(1) - 1
            } else minOf(clip.outFrame, clip.inFrame + clip.durationFrames.coerceAtLeast(1) - 1)
            val clipEndUs = (effectiveOutFrame + 1L).coerceAtLeast(clip.inFrame + 1L) * 1_000_000L / fps.coerceIn(1, 60)

            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clipStartUs / 1000L)
                .apply { if (clipEndUs != null) setEndPositionMs(clipEndUs / 1000L) }
                .build()
            val media = MediaItem.Builder()
                .setUri(Uri.fromFile(File(clip.uri)))
                .setClippingConfiguration(clipping)
                .build()

            val sourceDurationUs = if (clipEndUs != null) (clipEndUs - clipStartUs).coerceAtLeast(1L) else 0L
            val fadeInUs = clip.fadeInFrames.coerceAtLeast(0).toLong() * 1_000_000L / fps.coerceIn(1, 60)
            val fadeOutUs = clip.fadeOutFrames.coerceAtLeast(0).toLong() * 1_000_000L / fps.coerceIn(1, 60)
            val gain = DefaultGainProvider.Builder(clip.volume.coerceIn(0f, 4f)).apply {
                if (fadeInUs > 0) addFadeAt(0L, fadeInUs, DefaultGainProvider.FADE_IN_LINEAR)
                if (fadeOutUs > 0 && sourceDurationUs > fadeOutUs) {
                    addFadeAt(sourceDurationUs - fadeOutUs, fadeOutUs, DefaultGainProvider.FADE_OUT_LINEAR)
                }
            }.build()
            val effects = Effects(
                listOf(ToInt16PcmAudioProcessor(), GainProcessor(gain)),
                emptyList()
            )
            val edited = EditedMediaItem.Builder(media).setEffects(effects).build()
            val builder = EditedMediaItemSequence.Builder(setOf(androidx.media3.common.C.TRACK_TYPE_AUDIO))
            if (startUs > 0) builder.addGap(startUs)
            builder.addItem(edited)
            sequences += builder.build()
        }

        output.parentFile?.mkdirs()
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                onComplete(Result.success(Unit))
            }
            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                onComplete(Result.failure(exportException))
            }
        }
        try {
            val transformer = Transformer.Builder(context)
                .addListener(listener)
                .build()
            transformer.start(Composition.Builder(sequences).build(), output.absolutePath)
        } catch (t: Throwable) {
            onComplete(Result.failure(t))
        }
    }
}