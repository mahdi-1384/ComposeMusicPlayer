package ir.avesta.musicplayer.data.helpers

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import java.io.File

class MusicPlayer {
    private var _mediaPlayer: MediaPlayer? = null

    private val mediaPlayer get() = _mediaPlayer!!

    fun play(context: Context, source: String, onPrepared: () -> Unit) {
        stopIfPlaying()
        _mediaPlayer = MediaPlayer.create(context, source.toUri())

        mediaPlayer.start()

        onPrepared()
    }

    val duration get() = mediaPlayer.duration
    val currentPosition get() = mediaPlayer.currentPosition

    fun setOnPreparedListener(listener: MediaPlayer.OnPreparedListener) = mediaPlayer.setOnPreparedListener(listener)

    //the code is in try-catch because stop() method will throw exception if the player is already stopped
    private fun stopIfPlaying() {
        try {

            _mediaPlayer?.let { stop() }
        } catch (e: Exception) {}
    }

    //if isPlaying is true, pause the music, otherwise, continues
    fun playPause() {
        if (mediaPlayer.isPlaying)
            mediaPlayer.pause()
        else
            mediaPlayer.start()
    }

    fun stop() {
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    val isPlaying get() = mediaPlayer.isPlaying
}

fun String.toUri(): Uri = Uri.fromFile(File(this))





