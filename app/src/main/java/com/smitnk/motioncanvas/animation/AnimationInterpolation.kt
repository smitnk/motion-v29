package com.smitnk.motioncanvas.animation

enum class Easing { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }
object AnimationInterpolation {
    fun value(start: Float, end: Float, t: Float, easing: Easing = Easing.LINEAR): Float {
        val x = t.coerceIn(0f, 1f)
        val k = when(easing) { Easing.LINEAR -> x; Easing.EASE_IN -> x*x; Easing.EASE_OUT -> 1f-(1f-x)*(1f-x); Easing.EASE_IN_OUT -> if(x<.5f) 2*x*x else 1f-((-2*x+2).let{it*it}/2f) }
        return start + (end-start)*k
    }
}