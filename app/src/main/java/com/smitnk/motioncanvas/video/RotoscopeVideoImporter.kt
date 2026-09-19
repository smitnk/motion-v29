package com.smitnk.motioncanvas.video

import android.content.ContentResolver
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

object RotoscopeVideoImporter {
    fun extractFrames(resolver: ContentResolver, uri: Uri, fps: Int, maxFrames: Int = 120): List<Bitmap> {
        val out = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()
        return try {
            val pfd = resolver.openFileDescriptor(uri, "r") ?: return emptyList()
            retriever.setDataSource(pfd.fileDescriptor); pfd.close()
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (durationMs <= 0L) return emptyList()
            val stepUs = 1_000_000L / fps.coerceIn(1, 60)
            var t = 0L
            while (t < durationMs * 1000L && out.size < maxFrames) {
                retriever.getFrameAtTime(t, MediaMetadataRetriever.OPTION_CLOSEST)?.let { out.add(it) }
                t += stepUs
            }
            out
        } finally { retriever.release() }
    }
}