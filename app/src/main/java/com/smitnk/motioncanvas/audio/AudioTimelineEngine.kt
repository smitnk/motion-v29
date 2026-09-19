package com.smitnk.motioncanvas.audio

data class AudioTrim(val inFrame: Int = 0, val outFrame: Int = Int.MAX_VALUE, val fadeInFrames: Int = 0, val fadeOutFrames: Int = 0)

object AudioTimelineEngine {
    fun visibleAt(clip: AudioClip, frame: Int): Boolean {
        if (clip.muted || clip.durationFrames <= 0) return false
        val local = frame - clip.startFrame
        return local in clip.inFrame..minOf(clip.durationFrames - 1, clip.outFrame)
    }

    fun volumeAt(clip: AudioClip, frame: Int): Float {
        if (!visibleAt(clip, frame)) return 0f
        val local = frame - clip.startFrame
        val end = minOf(clip.durationFrames - 1, clip.outFrame)
        var gain = clip.volume.coerceIn(0f, 2f)
        if (clip.fadeInFrames > 0) gain *= ((local - clip.inFrame + 1).coerceAtLeast(0).toFloat() / clip.fadeInFrames).coerceAtMost(1f)
        if (clip.fadeOutFrames > 0) gain *= ((end - local + 1).coerceAtLeast(0).toFloat() / clip.fadeOutFrames).coerceAtMost(1f)
        return gain.coerceIn(0f, 2f)
    }

    fun clipsAt(tracks: List<AudioTrack>, frame: Int): List<AudioClip> =
        tracks.filter { it.enabled }.flatMap { it.clips }.filter { visibleAt(it, frame) }

    fun timelineDurationFrames(tracks: List<AudioTrack>): Int =
        tracks.flatMap { it.clips }.maxOfOrNull { it.startFrame + minOf(it.durationFrames, it.outFrame + 1) } ?: 0
}