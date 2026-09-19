package com.smitnk.motioncanvas.animation

data class Exposure(val startFrame:Int, val duration:Int=1)

object ExposureTrackEngine {
    fun frameAt(exposures:List<Exposure>, frame:Int):Int {
        if(exposures.isEmpty()) return frame
        var cursor=0
        for(e in exposures){
            val end=cursor+e.duration.coerceAtLeast(1)
            if(frame<end) return e.startFrame
            cursor=end
        }
        return exposures.last().startFrame
    }
}