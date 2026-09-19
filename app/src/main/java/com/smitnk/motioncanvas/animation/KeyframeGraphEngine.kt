package com.smitnk.motioncanvas.animation

import kotlin.math.pow

data class GraphKeyframe(val frame: Int, val value: Float, val easing: TweenEasing = TweenEasing.LINEAR)

object KeyframeGraphEngine {
    fun evaluate(keys: List<GraphKeyframe>, frame: Int): Float {
        if (keys.isEmpty()) return 0f
        val sorted=keys.sortedBy{it.frame}
        if(frame<=sorted.first().frame) return sorted.first().value
        if(frame>=sorted.last().frame) return sorted.last().value
        val i=sorted.indexOfLast{it.frame<=frame}.coerceAtLeast(0)
        val a=sorted[i]; val b=sorted[i+1]
        val t=((frame-a.frame).toFloat()/(b.frame-a.frame).coerceAtLeast(1)).coerceIn(0f,1f)
        val e=TweenEngine.ease(t,a.easing)
        return a.value+(b.value-a.value)*e
    }
}