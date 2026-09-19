package com.smitnk.motioncanvas.video

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.smitnk.motioncanvas.audio.AudioTrack
import java.io.File

/**
 * Multi-track audio/video export using the Media3 Transformer API available in 1.4.1.
 * Each enabled audio clip is placed in its own audio sequence so clips can overlap
 * on the final composition timeline.
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
        val frameRate = fps.coerceIn(1, 60)
        val activeClips = tracks
            .filter { it.enabled }
            .flatMap { track -> track.clips.filter { clip -> !clip.muted && File(clip.uri).exists() } }

        if (activeClips.isEmpty()) {
            video.copyTo(output, overwrite = true)
            onComplete(Result.success(Unit))
            return
        }

        val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(video)))
            .setRemoveAudio(true)
            .build()

        val sequences = mutableListOf<EditedMediaItemSequence>()
        sequences += EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO))
            .addItem(videoItem)
            .build()

        for (clip in activeClips) {
            val startUs = clip.startFrame.coerceAtLeast(0).toLong() * 1_000_000L / frameRate
            val clipStartUs = clip.inFrame.coerceAtLeast(0).toLong() * 1_000_000L / frameRate
            val effectiveOutFrame = if (clip.outFrame == Int.MAX_VALUE) {
                clip.inFrame + clip.durationFrames.coerceAtLeast(1) - 1
            } else {
                minOf(clip.outFrame, clip.inFrame + clip.durationFrames.coerceAtLeast(1) - 1)
            }
            val clipEndUs = (effectiveOutFrame + 1L).coerceAtLeast(clip.inFrame + 1L) * 1_000_000L / frameRate

            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clipStartUs / 1000L)
                .setEndPositionMs(clipEndUs / 1000L)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.fromFile(File(clip.uri)))
                .setClippingConfiguration(clipping)
                .build()

            val editedAudio = EditedMediaItem.Builder(mediaItem).build()
            val audioBuilder = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
            if (startUs > 0L) audioBuilder.addGap(startUs)
            audioBuilder.addItem(editedAudio)
            sequences += audioBuilder.build()
        }

        output.parentFile?.mkdirs()
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                onComplete(Result.success(Unit))
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                onComplete(Result.failure(exportException))
            }
        }

        try {
            Transformer.Builder(context)
                .addListener(listener)
                .build()
                .start(Composition.Builder(sequences).build(), output.absolutePath)
        } catch (t: Throwable) {
            onComplete(Result.failure(t))
        }
    }
}
