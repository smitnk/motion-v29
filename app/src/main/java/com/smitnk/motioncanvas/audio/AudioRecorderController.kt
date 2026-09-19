package com.smitnk.motioncanvas.audio
import android.content.Context
import android.media.MediaRecorder
import java.io.File
class AudioRecorderController(private val context: Context) {
    private var recorder: MediaRecorder? = null
    var outputFile: File? = null; private set
    fun start(): File {
        check(recorder == null) { "Recording already active" }
        val file = File(context.cacheDir, "motioncanvas-${System.currentTimeMillis()}.m4a")
        val r = MediaRecorder(context)
        r.setAudioSource(MediaRecorder.AudioSource.MIC); r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); r.setAudioSamplingRate(44100); r.setAudioEncodingBitRate(128000)
        r.setOutputFile(file.absolutePath); r.prepare(); r.start(); recorder = r; outputFile = file; return file
    }
    fun stop(): File? { val r = recorder ?: return outputFile; return try { r.stop(); outputFile } finally { r.release(); recorder = null } }
    fun cancel() { recorder?.runCatching { stop() }; recorder?.release(); recorder = null; outputFile?.delete(); outputFile = null }
}