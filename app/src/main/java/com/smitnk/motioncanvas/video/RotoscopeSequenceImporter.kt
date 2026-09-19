package com.smitnk.motioncanvas.video

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlin.math.max

/**
 * Production-oriented video-to-animation import pipeline.
 * Samples exact timeline timestamps, bounds memory, and scales frames to the
 * animation canvas before handing them to the editor.
 */
object RotoscopeSequenceImporter {
    data class Result(
        val frames: List<Bitmap>,
        val fps: Int,
        val durationMs: Long,
        val sourceWidth: Int,
        val sourceHeight: Int
    )

    fun import(
        resolver: ContentResolver,
        uri: Uri,
        targetFps: Int,
        maxFrames: Int = 600,
        targetWidth: Int = 1280,
        targetHeight: Int = 720
    ): Result {
        val retriever = MediaMetadataRetriever()
        return try {
            resolver.openFileDescriptor(uri, "r")?.use { retriever.setDataSource(it.fileDescriptor) }
                ?: error("Unable to open video")
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            require(durationMs > 0) { "Video has no readable duration" }
            val sourceWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: targetWidth
            val sourceHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: targetHeight
            val fps = targetFps.coerceIn(1, 60)
            val nominalCount = max(1, ((durationMs * fps) / 1000L).toInt())
            val count = minOf(maxFrames.coerceAtLeast(1), nominalCount)
            val frames = ArrayList<Bitmap>(count)
            for (i in 0 until count) {
                val timeUs = i.toLong() * 1_000_000L / fps
                val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: continue
                frames += fitToCanvas(raw, targetWidth, targetHeight)
                if (raw !== frames.lastOrNull()) runCatching { if (!raw.isRecycled) raw.recycle() }
            }
            Result(frames, fps, durationMs, sourceWidth, sourceHeight)
        } finally {
            retriever.release()
        }
    }

    private fun fitToCanvas(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val scale = minOf(targetWidth.toFloat() / source.width, targetHeight.toFloat() / source.height)
        val w = max(1, (source.width * scale).toInt())
        val h = max(1, (source.height * scale).toInt())
        if (w == source.width && h == source.height) return source
        return Bitmap.createScaledBitmap(source, w, h, true)
    }
}