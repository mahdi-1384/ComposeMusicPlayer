package ir.avesta.musicplayer.ui.main

import android.util.Log
import androidx.lifecycle.*
import ir.avesta.musicplayer.data.Repository
import ir.avesta.musicplayer.data.dataSource.AudiosSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(private val repo: Repository) : ViewModel() {
    private var _musicsList = MutableLiveData<MutableList<Long>>(null)
    val musicsList: LiveData<MutableList<Long>>
        get() = _musicsList

    //This variable holds the currently playing music data
    private var _currentlyPlayingMusic = EventMutableLiveData<Long>(null)
    val currentlyPlayingMusic: LiveData<Event<Long>>
        get() = _currentlyPlayingMusic

    private var _playingMusicCurrentPosition = EventMutableLiveData(0)
    val playingMusicCurrentPosition: LiveData<Event<Int>>
        get() = _playingMusicCurrentPosition

    private var _playingMusicDuration = EventMutableLiveData(0)
    val playingMusicDuration: LiveData<Event<Int>>
        get() = _playingMusicDuration

    fun onPlayMusic(musicItem: Long) {
        _currentlyPlayingMusic.value = Event(musicItem)
    }

    fun onPlayNextMusic() {
        val currentMusicIndex = musicsList.value!!.indexOf(currentlyPlayingMusic.value?.data)
        val nextMusicIndex = if (currentMusicIndex == musicsList.value!!.size - 1) 0 else currentMusicIndex + 1
        val nextMusicId = musicsList.value!![nextMusicIndex]

        onPlayMusic(nextMusicId)
    }

    fun getDisplayNameById(id: Long, displayName: (String) -> Unit) {
        viewModelScope.launch {
            val result = repo.getDisplayNameById(id)

            withContext(Dispatchers.Main) {
                displayName(result)
            }
        }
    }

    fun getAlbumNameById(id: Long, albumName: (String) -> Unit) {
        viewModelScope.launch {
            val result = repo.getAlbumNameById(id)

            withContext(Dispatchers.Main) {
                albumName(result)
            }
        }
    }

    fun getDataById(id: Long, data: (String) -> Unit) {
        viewModelScope.launch {
            val result = repo.getDataById(id)

            withContext(Dispatchers.Main) {
                data(result)
            }
        }
    }

    fun getSingerNameById(id: Long, data: (String) -> Unit) {
        viewModelScope.launch {
            val result = repo.getSingerNameById(id)

            withContext(Dispatchers.Main) {
                data(result)
            }
        }
    }

    fun onLoadMusicsList(sortOrder: AudiosSortOrder = AudiosSortOrder.DISPLAY_NAME) {
        _musicsList.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val data = repo.getAllMusics(sortOrder)

            withContext(Dispatchers.Main) {
                _musicsList.value = mutableListOf()
                _musicsList.value!!.addAll(data)
            }
        }
    }
}

class MainViewModelFactory(private val repo: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(repo) as T
    }
}