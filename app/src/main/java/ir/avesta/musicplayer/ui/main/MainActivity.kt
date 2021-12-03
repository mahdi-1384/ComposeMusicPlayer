package ir.avesta.musicplayer.ui.main

import android.Manifest
import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.os.*
import android.util.Log
import android.util.LruCache
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ir.avesta.musicplayer.MyApp
import ir.avesta.musicplayer.data.dataSource.AudiosSortOrder
import ir.avesta.musicplayer.data.notifications.NotificationMusicPlayback
import ir.avesta.musicplayer.data.services.MusicPlayBackService
import ir.avesta.musicplayer.ui.play.Play
import ir.avesta.musicplayer.ui.play.elapsedTime
import ir.avesta.musicplayer.ui.theme.MusicPlayerTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(myApp.repository)
    }

    companion object {
        var activityIsRunning = false
    }

    private val musicPlayingHandler by lazy { Handler(Looper.getMainLooper()) }
    private val PlayNextMusicBroadcast = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            mainViewModel.onPlayNextMusic()
        }
    }

    private val MediaPlayerPreparedBroadcast = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            musicPlayingHandler.removeCallbacks(onMusicTickRunnable)
            musicPlayingHandler.post(onMusicTickRunnable)
        }
    }

    private val MediaPlayerStopBroadcast = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            musicPlayingHandler.removeCallbacks(onMusicTickRunnable)
        }
    }

    private val onMusicTickRunnable = object: Runnable {
        override fun run() {
            Log.d("myapplog", playbackService.currentPosition.elapsedTime)

            musicPlayingHandler.postDelayed(this, 1000)
        }
    }

    private val playbackServiceConnection by lazy { object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            playbackService = (binder as MusicPlayBackService.PlaybackBinder).serviceClass
            playbackService.playMusic(musicId = playingMusicItem, musicsList = mainViewModel.musicsList.value!!)
        }
        override fun onServiceDisconnected(name: ComponentName?) {}
    } }

    private lateinit var playbackService: MusicPlayBackService
    private var playingMusicItem = 0L

    @ExperimentalMaterialApi
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityIsRunning = true
        registerReceivers()

        setContent {
            val isDark by remember { mutableStateOf(false) }
            val navController = rememberNavController()
            var chosenMusicsSortOrder by remember { mutableStateOf(AudiosSortOrder.DISPLAY_NAME) }

            val readExternalStoragePermission = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = {
                    if (it)
                        mainViewModel.onLoadMusicsList(chosenMusicsSortOrder)
                }
            )

            DisposableEffect(chosenMusicsSortOrder) {
                readExternalStoragePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                onDispose { }
            }

            MusicPlayerTheme(darkTheme = isDark) {
                NavHost(navController = navController, startDestination = Destinations.Main) {

                    composable(route = Destinations.Main) {
                        Main(
                            onChangeMusicsSortOrder = { chosenMusicsSortOrder = it },
                            musicsList = mainViewModel.musicsList.observeAsState(),
                            onNavigateToSearch = { },
                            chosenSortOrder = chosenMusicsSortOrder,
                            onGetDisplayNameById = {id ->
                                var data = ""; mainViewModel.getDisplayNameById(id) { data = it }

                                data
                            },
                            onGetAlbumNameById = { id ->
                                var data = ""; mainViewModel.getAlbumNameById(id) { data = it }

                                data
                            },
                            onPlayMusic = {musicItem ->
                                navController.navigate(Destinations.Play)
                                mainViewModel.onPlayMusic(musicItem)
                            },
                            onGetDataById = { id ->
                                var data = ""; mainViewModel.getDataById(id) {
                                data = it
                            }; data
                            }
                        )
                    }

                    composable(
                        route = Destinations.Play
                    ) {
                        Play(
                            musicId = mainViewModel.currentlyPlayingMusic.observeAsState().value!!.data,
                            onGetDataById = { var data = ""; mainViewModel.getDataById(it) { data = it }; data },
                            onGetSingerNameById = { var data = ""; mainViewModel.getSingerNameById(it) { data = it }; data },
                            onGetDisplayNameById = { var data = ""; mainViewModel.getDisplayNameById(it) { data = it }; data },
                            onBack = { navController.navigateUp() },
                            duration = mainViewModel.playingMusicDuration.observeAsState().value?.data!!,
                            currentPosition = mainViewModel.playingMusicCurrentPosition.observeAsState().value?.data!!
                        )
                    }
                }
            }
        }

        //observers
        mainViewModel.currentlyPlayingMusic.observe(this, onMusicToPlayChanged)
    }

    private val onMusicToPlayChanged = EventObserver<Long> { musicData ->
        if (musicData == null) return@EventObserver

        playingMusicItem = musicData

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            foregroundServicePermissionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE)
        } else {
            runMusicPlayback()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        activityIsRunning = false
        unRegisterReceivers()
    }

    private fun registerReceivers() {
        registerReceiver(PlayNextMusicBroadcast, IntentFilter(NotificationMusicPlayback.Next))
        registerReceiver(MediaPlayerStopBroadcast, IntentFilter(NotificationMusicPlayback.Stop))
        LocalBroadcastManager.getInstance(this).registerReceiver(MediaPlayerPreparedBroadcast, IntentFilter(MusicPlayBackService.Prepare))
    }

    private fun unRegisterReceivers() {
        unregisterReceiver(PlayNextMusicBroadcast)
        unregisterReceiver(MediaPlayerStopBroadcast)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(MediaPlayerPreparedBroadcast)
    }

    private val foregroundServicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            if (!it) return@ActivityResultCallback

            runMusicPlayback()
        }
    )

    private fun bindToPlaybackService() = bindService(Intent(this, MusicPlayBackService::class.java), playbackServiceConnection, BIND_AUTO_CREATE)

    private fun runMusicPlayback() {
        if (MusicPlayBackService.isRunning && this::playbackService.isInitialized) {
            playbackService.playMusic(playingMusicItem, mainViewModel.musicsList.value!!)
            return
        }

        val serviceIntent = Intent(this, MusicPlayBackService::class.java)
        startService(serviceIntent)

        bindToPlaybackService()
    }
}

const val MusicData = "MusicData"

object Destinations {
    const val Main = "Main"
    const val Play = "Play"
}

val String.decode get() = URLDecoder.decode(this, StandardCharsets.UTF_8.toString())

inline fun <reified T> T.serialize() = Json.encodeToString<T>(this)

val Activity.myApp: MyApp
    get() = application as MyApp