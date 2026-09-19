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
        require(width > 0 && height > 0)
        output.parentFile?.mkdirs()

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
            setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var track = -1
        var muxerStarted = false
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        try {
            val info = MediaCodec.BufferInfo()
            frames.forEachIndexed { index, bitmap ->
                val inputIndex = codec.dequeueInputBuffer(10_000)
                require(inputIndex >= 0) { "Encoder input timeout" }
                val buffer = codec.getInputBuffer(inputIndex) ?: error("Missing encoder buffer")
                val yuv = rgbaToNv12(bitmap, width, height)
                buffer.clear(); buffer.put(yuv)
                codec.queueInputBuffer(inputIndex, 0, yuv.size, index * 1_000_000L / fps, 0)
                drain(codec, muxer, info) { t -> track = t; muxerStarted = true }
            }
            val eosIndex = codec.dequeueInputBuffer(10_000)
            if (eosIndex >= 0) codec.queueInputBuffer(eosIndex, 0, 0, frames.size * 1_000_000L / fps, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            drain(codec, muxer, info, true) { t -> track = t; muxerStarted = true }
        } finally {
            if (muxerStarted) muxer.stop()
            muxer.release(); codec.stop(); codec.release()
        }
    }

    private fun drain(codec: MediaCodec, muxer: MediaMuxer, info: MediaCodec.BufferInfo, wait: Boolean = false, onFormat: (Int) -> Unit) {
        while (true) {
            val out = codec.dequeueOutputBuffer(info, if (wait) 10_000 else 0)
            when {
                out == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!wait) return else continue
                out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val t = muxer.addTrack(codec.outputFormat)
                    currentTrack = t
                    onFormat(t)
                }
                out >= 0 -> {
                    val buffer = codec.getOutputBuffer(out) ?: continue
                    if (info.size > 0) muxer.writeSampleData(currentTrack, buffer, info)
                    codec.releaseOutputBuffer(out, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                }
            }
        }
    }

    private var currentTrack: Int = -1
    private fun rgbaToNv12(source: Bitmap, w: Int, h: Int): ByteArray {
        val pixels = IntArray(w * h); source.getPixels(pixels, 0, source.width, 0, 0, w, h)
        val out = ByteArray(w * h * 3 / 2); var yIndex = 0; var uvIndex = w * h
        for (j in 0 until h) for (i in 0 until w) {
            val c = pixels[j * w + i]; val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
            val y = ((66*r + 129*g + 25*b + 128) shr 8) + 16
            out[yIndex++] = y.coerceIn(0,255).toByte()
            if (j % 2 == 0 && i % 2 == 0) {
                val u = ((-38*r - 74*g + 112*b + 128) shr 8) + 128
                val v = ((112*r - 94*g - 18*b + 128) shr 8) + 128
                out[uvIndex++] = u.coerceIn(0,255).toByte(); out[uvIndex++] = v.coerceIn(0,255).toByte()
            }
        }
        return out
    }
}
