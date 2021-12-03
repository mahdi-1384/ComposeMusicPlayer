package ir.avesta.musicplayer.data.helpers

import android.graphics.Bitmap
import android.util.LruCache

class BitmapCacheHelper : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory()/1024 / 8).toInt()) {
    override fun sizeOf(key: String?, value: Bitmap?): Int {
        return value?.byteCount ?: 0 / 1024
    }
}