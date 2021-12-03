package ir.avesta.musicplayer.data.dataSource

interface AudioRepoInterface {

    suspend fun getDisplayNameById(id: Long): String

    suspend fun getDataById(id: Long): String

    suspend fun getAlbumNameById(id: Long): String

    suspend fun getSingerNameById(id: Long): String

    suspend fun getAllMusics(sortOrder: AudiosSortOrder): List<Long>
}