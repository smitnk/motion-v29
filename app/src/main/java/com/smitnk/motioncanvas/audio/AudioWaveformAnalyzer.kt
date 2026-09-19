package com.smitnk.motioncanvas.audio

import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Decodes short PCM windows into a normalized waveform envelope for timeline previews. */
object AudioWaveformAnalyzer {
    fun analyze(path: String, points: Int = 160): List<Float> = runCatching {
        val ex=MediaExtractor(); ex.setDataSource(path); var track=-1
        for(i in 0 until ex.trackCount){ val f=ex.getTrackFormat(i); if(f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/")==true){track=i;break} }
        if(track<0) return@runCatching emptyList<Float>(); ex.selectTrack(track)
        val samples=FloatArray(points); val buf=ByteBuffer.allocate(64*1024).order(ByteOrder.LITTLE_ENDIAN); var n=0
        while(n<points){ buf.clear(); val read=ex.readSampleData(buf,0); if(read<=0) break; buf.flip(); var sum=0.0; var count=0; while(buf.remaining()>=2){sum += kotlin.math.abs(buf.short.toInt()); count++}; if(count>0)samples[n]=(sum/count/32768.0).toFloat().coerceIn(0f,1f); n++; ex.advance() }
        ex.release(); samples.toList()
    }.getOrElse { emptyList() }
}