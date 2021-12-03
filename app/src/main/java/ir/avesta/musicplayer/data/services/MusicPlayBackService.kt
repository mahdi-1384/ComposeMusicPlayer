package ir.avesta.musicplayer.data.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Binder
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ir.avesta.musicplayer.MyApp
import ir.avesta.musicplayer.data.helpers.MusicPlayer
import ir.avesta.musicplayer.data.notifications.MusicItem
import ir.avesta.musicplayer.data.notifications.NotificationMusicPlayback
import ir.avesta.musicplayer.ui.main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MusicPlayBackService : Service() {
    inner class PlaybackBinder : Binder() {
        val serviceClass get() = this@MusicPlayBackService
    }

    companion object {
        const val NextMusic = "NextMusic"
        const val Prepare = "Prepare"
        var isRunning = false
    }

    private val NotificationId = 1
    private val repo by lazy { (application as MyApp).repository }

    private var musicItem: MusicItem? = null
        set(value) {
            field = value

            musicPlayer.play(this, field!!.data) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(Prepare))
            }
            startForeground(NotificationId, getNotification(field!!, musicPlayer.isPlaying))
        }

    lateinit var musicsList: List<Long>
    private lateinit var musicPlayer: MusicPlayer
    private var currentLyPlayingMusicId = 0L
        set(value) {
            field = value

            getMusicItem {
                musicItem = it
            }
        }

    private fun getMusicItem(result: (MusicItem) -> Unit) {
        val musicItem = MusicItem(id = currentLyPlayingMusicId)

        CoroutineScope(Dispatchers.IO).launch {
            val data = repo.getDataById(currentLyPlayingMusicId)
            val singerName = repo.getSingerNameById(currentLyPlayingMusicId)
            val musicName = repo.getDisplayNameById(currentLyPlayingMusicId)

            withContext(Dispatchers.Main) {
                musicItem.apply {
                    this.data = data
                    this.singerName = singerName
                    this.musicName = musicName
                }

                result(musicItem)
            }
        }
    }

    val musicDuration get() = musicPlayer.duration
    val currentPosition get() = musicPlayer.currentPosition

    private val StopBroadcast = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            stopAll()
        }
    }

    private val NextMusicBroadcast = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!MainActivity.activityIsRunning)
                playNextMusic()
        }
    }

    //stops service, notification and the music playback
    private fun stopAll() {
        musicPlayer.playPause()
        stopForeground(true)
        stopSelf()
    }

    private fun playNextMusic() {
        val currentMusicIndex = musicsList.indexOf(currentLyPlayingMusicId)
        val nextMusicIndex = if (currentMusicIndex == musicsList.size - 1) 0 else currentMusicIndex + 1

        currentLyPlayingMusicId = musicsList[nextMusicIndex]
    }

    override fun onCreate() {
        registerReceiver(StopBroadcast, IntentFilter(NotificationMusicPlayback.Stop))
        registerReceiver(NextMusicBroadcast, IntentFilter(NotificationMusicPlayback.Next))
        isRunning = true
    }

    override fun onDestroy() {
        unregisterReceiver(StopBroadcast)
        unregisterReceiver(NextMusicBroadcast)
        isRunning = false
    }

    fun playMusic(musicId: Long, musicsList: List<Long>) {
        if (!this::musicPlayer.isInitialized)
            musicPlayer = MusicPlayer()

        currentLyPlayingMusicId = musicId

        this.musicsList = musicsList
    }

    private fun getNotification(musicItem: MusicItem, isMusicPlaying: Boolean) = NotificationMusicPlayback.create(this, musicItem, isMusicPlaying)

    private val binderInstance by lazy { PlaybackBinder() }
    override fun onBind(intent: Intent?) = binderInstance
}




inline fun <reified S> String.deserialize() = Json.decodeFromString<S>(this)

fun String.isPermissionGranted(context: Context) =
    ContextCompat.checkSelfPermission(context, this) == PackageManager.PERMISSION_GRANTED