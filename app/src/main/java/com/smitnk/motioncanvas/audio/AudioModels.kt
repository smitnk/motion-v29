package com.smitnk.motioncanvas.audio

data class AudioClip(
    val id: String,
    val uri: String,
    val startFrame: Int,
    val durationFrames: Int,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val inFrame: Int = 0,
    val outFrame: Int = Int.MAX_VALUE,
    val fadeInFrames: Int = 0,
    val fadeOutFrames: Int = 0
)

data class AudioTrack(
    val id: String,
    val name: String = "Audio",
    val clips: MutableList<AudioClip> = mutableListOf(),
    val enabled: Boolean = true
)

object AudioTrackBridge {
    fun ensureTracks(tracks: MutableList<AudioTrack>, legacy: MutableList<AudioClip>) {
        if (tracks.isEmpty() && legacy.isNotEmpty()) {
            tracks.add(AudioTrack("track-1", "Audio 1", legacy.toMutableList()))
        }
        if (tracks.isNotEmpty()) {
            legacy.clear()
            legacy.addAll(tracks.flatMap { it.clips })
        }
    }
}