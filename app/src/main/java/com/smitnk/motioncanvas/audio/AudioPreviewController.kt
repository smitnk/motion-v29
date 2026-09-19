package com.smitnk.motioncanvas.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class AudioPreviewController(private val context: Context) {
    private var player: MediaPlayer? = null
    fun play(uri: Uri, loop: Boolean = false) { stop(); player=MediaPlayer.create(context,uri)?.apply{isLooping=loop;start()} }
    fun pause(){player?.pause()}
    fun resume(){player?.start()}
    fun stop(){player?.release();player=null}
}