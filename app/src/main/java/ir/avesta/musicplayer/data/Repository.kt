package ir.avesta.musicplayer.data

import androidx.navigation.compose.DialogNavigator
import ir.avesta.musicplayer.data.dataSource.AudioRepoInterface
import ir.avesta.musicplayer.data.dataSource.AudiosSortOrder

class Repository(private val audioHelper: AudioRepoInterface) : AudioRepoInterface {

    override suspend fun getAlbumNameById(id: Long) = audioHelper.getAlbumNameById(id)

    override suspend fun getSingerNameById(id: Long) = audioHelper.getSingerNameById(id)

    override suspend fun getDisplayNameById(id: Long) = audioHelper.getDisplayNameById(id)

    override suspend fun getDataById(id: Long) = audioHelper.getDataById(id)

    override suspend fun getAllMusics(sortOrder: AudiosSortOrder) = audioHelper.getAllMusics(sortOrder)
}