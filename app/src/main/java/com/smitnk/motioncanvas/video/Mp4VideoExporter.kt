package com.smitnk.motioncanvas.video

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/** Lightweight H.264 MP4 exporter. Frames are supplied in display order. */
object Mp4VideoExporter {
    fun export(frames: List<Bitmap>, fps: Int, output: File): Result<Unit> = runCatching {
        require(frames.isNotEmpty()) { "No frames to export" }
        require(fps in 1..120) { "FPS must be 1..120" }

        val first = frames.first()
        val width = first.width and 0xFFFFFFFE.toInt()
        val height = first.height and 0xFFFFFFFE.toInt()
        require(width > 0 && height > 0) { "Invalid frame dimensions" }
        output.parentFile?.mkdirs()

        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            width,
            height
        ).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
            setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val info = MediaCodec.BufferInfo()

            for ((index, bitmap) in frames.withIndex()) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                require(inputIndex >= 0) { "Encoder input timeout" }
                val input = codec.getInputBuffer(inputIndex) ?: error("Missing encoder buffer")
                val yuv = rgbaToNv12(bitmap, width, height)
                input.clear()
                input.put(yuv)
                codec.queueInputBuffer(
                    inputIndex,
                    0,
                    yuv.size,
                    index * 1_000_000L / fps,
                    0
                )
                drain(codec, muxer, info, false) { newTrack ->
                    trackIndex = newTrack
                    muxerStarted = true
                }
            }

            val eosIndex = codec.dequeueInputBuffer(10_000)
            if (eosIndex >= 0) {
                codec.queueInputBuffer(
                    eosIndex,
                    0,
                    0,
                    frames.size * 1_000_000L / fps,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }
            drain(codec, muxer, info, true) { newTrack ->
                trackIndex = newTrack
                muxerStarted = true
            }
        } finally {
            if (muxerStarted) muxer.stop()
            muxer.release()
            codec.stop()
            codec.release()
        }
    }

    private fun drain(
        codec: MediaCodec,
        muxer: MediaMuxer,
        info: MediaCodec.BufferInfo,
        waitForData: Boolean,
        onFormat: (Int) -> Unit
    ) {
        while (true) {
            val result = codec.dequeueOutputBuffer(info, if (waitForData) 10_000 else 0)
            if (result == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!waitForData) return
                continue
            }
            if (result == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newTrack = muxer.addTrack(codec.outputFormat)
                activeTrack = newTrack
                onFormat(newTrack)
                continue
            }
            if (result < 0) continue

            val output = codec.getOutputBuffer(result)
            if (output != null && info.size > 0) {
                // The format must have been announced before sample data can be written.
                // The caller's callback stores the current track in the enclosing export scope.
                writePendingSample(muxer, output, info)
            }
            codec.releaseOutputBuffer(result, false)

            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
        }
    }

    private fun writePendingSample(
        muxer: MediaMuxer,
        buffer: java.nio.ByteBuffer,
        info: MediaCodec.BufferInfo
    ) {
        // Track selection is kept in a field only while this synchronous exporter runs.
        if (activeTrack >= 0) muxer.writeSampleData(activeTrack, buffer, info)
    }

    private var activeTrack: Int = -1

    private fun rgbaToNv12(source: Bitmap, w: Int, h: Int): ByteArray {
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, source.width, 0, 0, w, h)
        val out = ByteArray(w * h * 3 / 2)
        var yIndex = 0
        var uvIndex = w * h

        for (j in 0 until h) {
            for (i in 0 until w) {
                val c = pixels[j * w + i]
                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                out[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    out[uvIndex++] = u.coerceIn(0, 255).toByte()
                    out[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
        return out
    }
}
