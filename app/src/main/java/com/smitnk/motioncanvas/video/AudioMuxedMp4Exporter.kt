package com.smitnk.motioncanvas.video

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/**
 * Muxes an existing H.264 MP4 with an AAC/M4A audio clip without re-encoding.
 * Apache-2.0 Media3/AOSP concepts are used as the media-pipeline reference;
 * this implementation uses Android platform MediaExtractor/MediaMuxer.
 */
object AudioMuxedMp4Exporter {
    fun mux(video: File, audio: File, output: File, audioStartUs: Long = 0L): Result<Unit> = runCatching {
        require(video.exists()) { "Video file does not exist" }
        require(audio.exists()) { "Audio file does not exist" }
        output.parentFile?.mkdirs()

        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        videoExtractor.setDataSource(video.absolutePath)
        audioExtractor.setDataSource(audio.absolutePath)
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        try {
            val videoTrack = selectTrack(videoExtractor, "video/")
            require(videoTrack >= 0) { "No video track found" }
            val audioTrack = selectTrack(audioExtractor, "audio/")
            require(audioTrack >= 0) { "No audio track found" }
            videoExtractor.selectTrack(videoTrack)
            audioExtractor.selectTrack(audioTrack)

            val outVideo = muxer.addTrack(videoExtractor.getTrackFormat(videoTrack))
            val outAudio = muxer.addTrack(audioExtractor.getTrackFormat(audioTrack))
            muxer.start()

            copySamples(videoExtractor, muxer, outVideo, 0L, Long.MAX_VALUE)
            copySamples(audioExtractor, muxer, outAudio, audioStartUs, Long.MAX_VALUE)
        } finally {
            runCatching { muxer.stop() }
            muxer.release()
            videoExtractor.release()
            audioExtractor.release()
        }
    }

    private fun selectTrack(extractor: MediaExtractor, prefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith(prefix)) return i
        }
        return -1
    }

    private fun copySamples(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        outputTrack: Int,
        timestampOffsetUs: Long,
        maxDurationUs: Long
    ) {
        val buffer = java.nio.ByteBuffer.allocateDirect(1024 * 1024)
        val info = android.media.MediaCodec.BufferInfo()
        while (true) {
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            val pts = extractor.sampleTime
            if (pts < 0) break
            if (pts <= maxDurationUs) {
                info.offset = 0
                info.size = size
                info.presentationTimeUs = (pts + timestampOffsetUs).coerceAtLeast(0L)
                info.flags = extractor.sampleFlags
                buffer.position(0)
                buffer.limit(size)
                muxer.writeSampleData(outputTrack, buffer, info)
            }
            extractor.advance()
        }
    }
}