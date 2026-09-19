package com.smitnk.motioncanvas.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import java.io.File
import kotlin.math.roundToInt

/** Keeps video frame position and multiple audio clips on one frame-based clock. */
class SharedAudioVideoClock(private val context: Context) {
    private val players = mutableMapOf<String, MediaPlayer>()
    private var playing = false
    private var fps = 12

    fun prepare(tracks: List<AudioTrack>, fps: Int) {
        release()
        this.fps = fps.coerceIn(1, 60)
        tracks.filter { it.enabled }.flatMap { it.clips }.distinctBy { it.id }.forEach { clip ->
            runCatching {
                val mp = MediaPlayer.create(context, Uri.fromFile(File(clip.uri))) ?: return@runCatching
                mp.isLooping = false
                players[clip.id] = mp
            }
        }
    }

    fun startAtFrame(frame: Int) {
        seekToFrame(frame)
        players.values.forEach { runCatching { it.start() } }
        playing = true
    }

    fun pause() { players.values.forEach { runCatching { it.pause() } }; playing = false }

    fun stop() { players.values.forEach { runCatching { it.stop() } }; playing = false }

    fun seekToFrame(frame: Int) {
        val msPerFrame = 1000.0 / fps
        players.forEach { (id, mp) ->
            val clip = currentClips[id] ?: return@forEach
            val local = frame - clip.startFrame
            if (local < clip.inFrame || local > minOf(clip.durationFrames - 1, clip.outFrame)) {
                runCatching { if (mp.isPlaying) mp.pause() }
            } else {
                val position = ((local - clip.inFrame) * msPerFrame).roundToInt().coerceAtLeast(0)
                runCatching { mp.seekTo(position); if (playing && !mp.isPlaying) mp.start() }
            }
        }
    }

    private val currentClips = mutableMapOf<String, AudioClip>()
    fun bindClips(tracks: List<AudioTrack>) {
        currentClips.clear()
        tracks.flatMap { it.clips }.forEach { currentClips[it.id] = it }
    }

    fun release() {
        players.values.forEach { runCatching { it.release() } }
        players.clear(); currentClips.clear(); playing = false
    }
}