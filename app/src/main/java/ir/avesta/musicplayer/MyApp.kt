package ir.avesta.musicplayer

import android.app.Application
import ir.avesta.musicplayer.data.Repository
import ir.avesta.musicplayer.data.dataSource.AudioHelper
import ir.avesta.musicplayer.data.dataSource.AudioRepoInterface

class MyApp : Application() {

    private val audioHelperInterface: AudioRepoInterface by lazy { AudioHelper(contentResolver) }
    val repository by lazy { Repository(audioHelperInterface) }
}