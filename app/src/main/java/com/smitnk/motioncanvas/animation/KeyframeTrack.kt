package com.smitnk.motioncanvas.animation
data class Keyframe<T>(val frame: Int, val value: T)
class KeyframeTrack<T>(initial: T) {
    private val keyframes = mutableListOf(Keyframe(0, initial))
    val frames: List<Keyframe<T>> get() = keyframes.toList()
    fun set(frame: Int, value: T) { keyframes.removeAll { it.frame == frame }; keyframes.add(Keyframe(frame.coerceAtLeast(0), value)); keyframes.sortBy { it.frame } }
    fun remove(frame: Int) { keyframes.removeAll { it.frame == frame } }
    fun previous(frame: Int): Keyframe<T>? = keyframes.lastOrNull { it.frame <= frame }
    fun next(frame: Int): Keyframe<T>? = keyframes.firstOrNull { it.frame >= frame }
}