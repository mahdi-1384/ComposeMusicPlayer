package ir.avesta.musicplayer.data.dataSource

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import ir.avesta.musicplayer.R

//query with this column and get the other columns data
const val keyColumn = MediaStore.Audio.AudioColumns._ID
enum class DataCategory(val projection: Array<String>?, val selection: String?) {

    AllMusics(arrayOf(keyColumn), null),

    Data(arrayOf(MediaStore.Audio.AudioColumns.DATA), "$keyColumn = ?"),

    AlbumName(arrayOf(MediaStore.Audio.AudioColumns.ALBUM), "$keyColumn = ?"),

    SingerName(arrayOf(MediaStore.Audio.AudioColumns.ARTIST), "$keyColumn = ?"),

    DisplayName(arrayOf(MediaStore.Audio.AudioColumns.DISPLAY_NAME), "$keyColumn = ?")
}

class AudioHelper(private val contentResolver: ContentResolver) : AudioRepoInterface {
    private fun getCursor(
        dataCategory: DataCategory,
        selectionArgs: Array<String>? = null,
        sortOrder: String = AudiosSortOrder.DISPLAY_NAME.column

    ) = contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        dataCategory.projection,
        dataCategory.selection,
        selectionArgs,
        sortOrder
    )

    override suspend fun getDisplayNameById(id: Long): String {
        val cursor = getCursor(DataCategory.DisplayName, arrayOf("$id"))
        var result = ""

        cursor?.let {
            if (cursor.moveToNext()) {
                val displayName = cursor.mGetString(DataCategory.DisplayName.projection!![0])
                result = displayName
            }
        }

        return result
    }

    override suspend fun getDataById(id: Long): String {
        val cursor = getCursor(DataCategory.Data, selectionArgs = arrayOf("$id"))
        var result = ""

        cursor?.let {
            if (cursor.moveToNext()) {
                val data = cursor.mGetString(DataCategory.Data.projection!![0])
                result = data
            }
        }

        return result
    }

    override suspend fun getAlbumNameById(id: Long): String {
        val cursor = getCursor(DataCategory.AlbumName, arrayOf("$id"))
        var result = ""

        cursor?.let {
            if (cursor.moveToNext()) {
                val album = cursor.mGetString(DataCategory.AlbumName.projection!![0])
                result = album
            }
        }

        return result
    }

    override suspend fun getSingerNameById(id: Long): String {
        val cursor = getCursor(DataCategory.SingerName, arrayOf("$id"))
        var result = ""

        cursor?.let {
            if (cursor.moveToNext()) {
                val singer = cursor.mGetString(DataCategory.SingerName.projection!![0])
                result = singer
            }
        }

        return result
    }

    override suspend fun getAllMusics(sortOrder: AudiosSortOrder): List<Long> {
        val cursor = getCursor(DataCategory.AllMusics, sortOrder = sortOrder.column)
        val result = mutableListOf<Long>()

        cursor?.let {
            if (cursor.moveToFirst()) {
                while (cursor.moveToNext()) {
                    val id = cursor.mGetLong(DataCategory.AllMusics.projection!![0])
                    result.add(id)
                }
            }
        }

        return result.toList()
    }
}

fun Cursor.mGetLong(columnName: String) = this.getLong(this.getColumnIndex(columnName))
fun Cursor.mGetString(columnName: String) = this.getString(this.getColumnIndex(columnName))

enum class AudiosSortOrder(val column: String, val textResource: Int) {
    DATE_ADDED(MediaStore.Audio.AudioColumns.DATE_ADDED, R.string.orderByDateAdded),
    DISPLAY_NAME(MediaStore.Audio.AudioColumns.DISPLAY_NAME, R.string.orderByDisplayName),
}

fun getMimeTypeFromExtension(extension: String) = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)