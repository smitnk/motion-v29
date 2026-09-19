package com.smitnk.motioncanvas.audio

data class Waveform(val samples:FloatArray,val sampleRate:Int)
object WaveformAnalyzer {
    fun downsample(samples:FloatArray,bins:Int):FloatArray{
        if(samples.isEmpty()||bins<=0)return FloatArray(0)
        val out=FloatArray(bins)
        for(i in 0 until bins){
            val a=i*samples.size/bins; val b=((i+1)*samples.size/bins).coerceAtMost(samples.size)
            var peak=0f
            for(j in a until b) peak=maxOf(peak,kotlin.math.abs(samples[j]))
            out[i]=peak
        }
        return out
    }
}