package ir.avesta.musicplayer.ui.play

import android.graphics.Bitmap
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.avesta.musicplayer.R
import ir.avesta.musicplayer.ui.main.Icon
import ir.avesta.musicplayer.ui.main.Image
import ir.avesta.musicplayer.ui.main.musicThumbnail

@ExperimentalAnimationApi
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Play(
    musicId: Long?,
    onGetDataById: (Long) -> String,
    onGetDisplayNameById: (Long) -> String,
    onGetSingerNameById: (Long) -> String,
    duration: Int,
    currentPosition: Int,
    onBack: () -> Unit
) {
    var isEditModeActive by remember { mutableStateOf(false) }

    val onChangeEditMode = {
        isEditModeActive = !isEditModeActive
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nonRippleClickable(onClick = onChangeEditMode)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {

            val musicName by produceState(initialValue = "") {
                value = onGetDisplayNameById(musicId!!)
            }

            val singerName by produceState(initialValue = "") {
                value = onGetSingerNameById(musicId!!)
            }

            OptionsMenu(modifier = Modifier.align(Alignment.CenterStart), onClick = { })

            Title(modifier = Modifier.align(Alignment.Center), displayName = musicName, singerName = singerName)

            BackIcon(modifier = Modifier.align(Alignment.CenterEnd), onClick = onBack)
        }

        val data by produceState<String?>(initialValue = null, musicId) {
            value = onGetDataById(musicId!!)
        }

        val thumbnail by produceState<Bitmap?>(null, musicId) {
            value = data.musicThumbnail
        }

        ThumbnailImage(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.7f)
                .aspectRatio(1f),
            thumbnail = thumbnail,
            isEditModeActive = isEditModeActive,
            onChangeEditMode = onChangeEditMode
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            var playMode by remember { mutableStateOf(PlayMode.RepeatOne) }
            var isFavorite by remember { mutableStateOf(true) }

            val changePlayMode = {
                val currentIndex = playMode.ordinal
                val targetIndex = if (playMode.isLast) 0 else currentIndex + 1
                playMode = PlayMode.values()[targetIndex]
            }

            Panel(
                onDisplayPlaylists = { },
                isFavorite = isFavorite,
                onFavoriteChange = { isFavorite = !isFavorite },
                currentPlayMode = playMode,
                onChangePlayMode = changePlayMode
            )

            MusicSlider(
                currentPosition = currentPosition,
                duration = duration
            )
            MusicButtons()
        }
    }
}

enum class PlayMode(val iconResource: Int) {
    RepeatOne(R.drawable.replay_one),
    RepeatAll(R.drawable.replay_all),
    AccidentalPlay(R.drawable.straight_shuffle);

    val isLast: Boolean
        get() = (ordinal == values().size - 1)

    companion object {
        val resources: List<Int>
            get() = values().toList().map { it.iconResource }
    }
}

@Composable
fun Panel(
    onDisplayPlaylists: () -> Unit,
    isFavorite: Boolean,
    onFavoriteChange: () -> Unit,
    currentPlayMode: PlayMode,
    onChangePlayMode: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(0.7f)) {
        val iconsTint = Color.DarkGray

        Icon(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onDisplayPlaylists)
                .scale(0.9f)
            ,
            painter = painterResource(R.drawable.playlists),
            tint = iconsTint
        )

        val favoriteIconModifier = Modifier
            .align(Alignment.Center)
            .clickable(onClick = onFavoriteChange)
        if (isFavorite)
            Icon(modifier = favoriteIconModifier, imageVector = Icons.Filled.Favorite, tint = Color(0xFFFF0000))
        else
            Icon(modifier = favoriteIconModifier, painter = painterResource(R.drawable.border_favorite), tint = iconsTint)

        MultiIconToggleButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(30.dp),
            iconsResources = PlayMode.resources,
            currentIconIndex = currentPlayMode.ordinal,
            onClick = onChangePlayMode,
            tint = iconsTint
        )
    }
}

data class Time(
    val hours: Int,
    val minutes: Int,
    val seconds: Int
) {
    override fun toString() = "$hours:$minutes:$seconds"
}

@Composable
fun MusicSlider(currentPosition: Int, duration: Int) {
    val label: @Composable (String) -> Unit = {
        Text(text = it, color = Color.LightGray, fontSize = 10.sp)
    }
    val (progress, onProgressChange) = remember { mutableStateOf(0f) }

    Row(modifier = Modifier.fillMaxWidth(0.9f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
        label(duration.elapsedTime)

        Slider(
            modifier = Modifier.weight(1f),
            progress = progress,
            onProgressChange = onProgressChange
        )

        label(currentPosition.elapsedTime)
    }
}

@Composable
fun Slider(modifier: Modifier = Modifier, progress: Float, onProgressChange: (Float) -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val thumbColor = Color(0xFF512ABD)

        Slider(
            modifier = modifier,
            value = progress,
            onValueChange = onProgressChange,
            colors = SliderDefaults.colors(thumbColor = thumbColor, activeTrackColor = thumbColor.copy(alpha = 0.7f), inactiveTrackColor = Color.LightGray.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun MusicButtons() {

}

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun ThumbnailImage(
    modifier: Modifier = Modifier,
    thumbnail: Bitmap?,
    isEditModeActive: Boolean,
    onChangeEditMode: () -> Unit
) {
    val imageModifier = Modifier.fillMaxSize()

    Surface(
        modifier = modifier,
        elevation = 4.dp,
        shape = RoundedCornerShape(6.dp),
        onClick = onChangeEditMode,
        indication = null
    ) {
        val editInstruction = @Composable {
            Text(
                text = stringResource(R.string.hold_to_choose_the_local_music_text),
                color = Color(0xFF178DF3)
            )
        }

        val image = @Composable {
            if (thumbnail == null)
                Image(modifier = imageModifier, painter = painterResource(R.drawable.music_logo))
            else
                Image(modifier = imageModifier, bitmap = thumbnail.asImageBitmap())
        }

        val enterAnim = fadeIn(animationSpec = tween(600))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnimatedVisibility(visible = !isEditModeActive, enter = enterAnim, exit = fadeOut()) {
                image()
            }
            AnimatedVisibility(visible = isEditModeActive, enter = enterAnim, exit = fadeOut()) {
                editInstruction()
            }
        }
    }
}

@Composable
fun OptionsMenu(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IntIcon(modifier, resource = R.drawable.options_menu, onClick = onClick, tint = Color.DarkGray.copy(alpha = 0.8f))
}

@Composable
fun Title(modifier: Modifier = Modifier, displayName: String, singerName: String) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(modifier = modifier, text = displayName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(modifier = modifier, text = singerName, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun BackIcon(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IntIcon(modifier = modifier, resource = R.drawable.down_arrow, onClick = onClick)
}

/**Loading an icon by resource*/
@Composable
fun IntIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Gray,
    resource: Int,
    onClick: () -> Unit
) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(painter = painterResource(resource), tint = tint)
    }
}

@Composable
fun Modifier.nonRippleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }

    return this.then(
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    )
}

@Composable
fun MultiIconToggleButton(
    modifier: Modifier = Modifier,
    iconsResources: List<Int>,
    hasRipple: Boolean = false,
    currentIconIndex: Int,
    onClick: () -> Unit,
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    if (hasRipple)
        IconButton(modifier = modifier, onClick = onClick) {
            Icon(painter = painterResource(iconsResources[currentIconIndex]), tint = tint)
        }
    else
        Icon(
            modifier = modifier.clickable(onClick = onClick),
            painter = painterResource(iconsResources[currentIconIndex]),
            tint = tint
        )
}

val Int.elapsedTime: String
    get() {
        val seconds = this / 1000

        if (seconds < 60)
            return "$seconds"
        else
            return DateUtils.formatElapsedTime(seconds.toLong())
    }