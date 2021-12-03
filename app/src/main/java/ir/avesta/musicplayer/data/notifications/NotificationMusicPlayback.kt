package ir.avesta.musicplayer.data.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.avesta.musicplayer.R
import ir.avesta.musicplayer.ui.main.musicThumbnail
import kotlinx.serialization.Serializable

@Serializable
data class MusicItem(
    var id: Long = 0L,
    var data: String = "",
    var musicName: String = "",
    var singerName: String = "",
)

class NotificationMusicPlayback private constructor(
    private val notificationManager: NotificationManagerCompat,
    private val notification: Notification,
    private val notificationChannel: NotificationChannel?,

) {

    companion object {
        const val Stop = "Stop"
        const val PlayPause = "PlayPause"
        const val Next = "Next"

        @SuppressLint("UnspecifiedImmutableFlag")
        fun create(context: Context, musicItem: MusicItem, isMusicPlaying: Boolean): Notification {
            // TODO: this drawable needs to be changed later
            val smallIcon = R.drawable.music
            val channelId = NotificationMusicPlayback::class.java.name
            val channelName = context.resources.getString(R.string.musicPlayBack)

            fun getPendingIntent(requestCode: Int, action: String) = PendingIntent.getBroadcast(context, requestCode, Intent(action), PendingIntent.FLAG_UPDATE_CURRENT)
            val remoteView = RemoteViews(context.packageName, R.layout.playback_notification_layout).apply {
                setTextViewText(R.id.musicNameTv, musicItem.musicName)
                setTextViewText(R.id.singerNameTv, musicItem.singerName)

                val musicThumbnail = musicItem.data.musicThumbnail
                musicThumbnail?.let { setImageViewBitmap(R.id.musicThumbnailImg, it) }

                setOnClickPendingIntent(R.id.stopImg, getPendingIntent(1, Stop))
                setOnClickPendingIntent(R.id.nextImg, getPendingIntent(0, Next))
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(smallIcon)
                .setStyle(androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle())
                .setCustomContentView(remoteView)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                NotificationManagerCompat.from(context).apply {
                    createNotificationChannel(notificationChannel)
                }
            }

            return notification
        }
    }
}


fun Int.toBitmap(resources: Resources) = BitmapFactory.decodeResource(resources, this)