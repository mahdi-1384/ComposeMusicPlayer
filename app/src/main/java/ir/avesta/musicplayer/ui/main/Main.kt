package ir.avesta.musicplayer.ui.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.avesta.musicplayer.R
import ir.avesta.musicplayer.data.helpers.BitmapCacheHelper
import ir.avesta.musicplayer.data.dataSource.AudiosSortOrder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt

val ContentsTopBarHeight = 56.dp
val DividerHeight = 4.dp
val SearchBarHeight = 50.dp
val PanelHeight = 50.dp
val lineDividerHeight = 0.6.dp

@Composable
fun Main(
    musicsList: State<List<Long>?>,
    onNavigateToSearch: () -> Unit,
    chosenSortOrder: AudiosSortOrder,
    onChangeMusicsSortOrder: (AudiosSortOrder) -> Unit,
    onGetDisplayNameById: (Long) -> String,
    onGetAlbumNameById: (Long) -> String,
    onPlayMusic: (Long) -> Unit,
    onGetDataById: (Long) -> String
) {
    val ContentsTopBarHeightPx = with(LocalDensity.current) { ContentsTopBarHeight.roundToPx() }
    val DividerHeightPx = with(LocalDensity.current) { DividerHeight.roundToPx() }
    val SearchBarHeightPx = with(LocalDensity.current) { SearchBarHeight.roundToPx() }
    var offset by remember { mutableStateOf(SearchBarHeightPx.toFloat()) }
    val changeSortOrderDialogIsVisible by remember { mutableStateOf(false) }

    SortOrderDialog(
        visible = changeSortOrderDialogIsVisible,
        chosenSortorder = chosenSortOrder,
        onItemClick = onChangeMusicsSortOrder
    )

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {

            offset = (offset + available.y).coerceIn(
                SearchBarHeightPx - ContentsTopBarHeightPx.toFloat() - DividerHeightPx,
                SearchBarHeightPx.toFloat()
            )

            return Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {

        Content(
            modifier = Modifier.align(Alignment.Center),
            musicsList = musicsList.value,
            onGetDisplayNameById = onGetDisplayNameById,
            onGetAlbumNameById = onGetAlbumNameById,
            onGetDataById = onGetDataById,
            onPlayMusic = onPlayMusic
        )

        Panel(
            offset = offset.roundToInt() + ContentsTopBarHeightPx + DividerHeightPx,
            onAppsClick = {},
            onSwapClick = { /*changeSortOrderDialogIsVisible = true*/ },
            onCheckAllClick = {},
            onShuffleClick = {},
        )

        val PanelHeightPx = with(LocalDensity.current) { PanelHeight.roundToPx() }
        Divider(
            modifier = Modifier.offsetY(offset.roundToInt() + ContentsTopBarHeightPx + DividerHeightPx + PanelHeightPx),
            color = Color.LightGray.copy(alpha = 0.6f),
            thickness = lineDividerHeight
        )

        Divider(
            color = Color(0xFFF7F7F7),
            thickness = DividerHeight,
            modifier = Modifier.offsetY(offset.roundToInt() + ContentsTopBarHeightPx)
        )

        ContentsTopBar(offset = offset.roundToInt())

        SearchBar(onClick = onNavigateToSearch)
    }
}

@Composable
fun SortOrderDialog(
    visible: Boolean,
    chosenSortorder: AudiosSortOrder,
    onItemClick: (AudiosSortOrder) -> Unit
) {
    if (!visible) return

    var focusedItem by remember { mutableStateOf(chosenSortorder) }
    val offsetDp = with(LocalDensity.current) { 32.dp.roundToPx() }

    val space: @Composable (Dp) -> Unit = { space ->
        Spacer(modifier = Modifier.height(space))
    }
    val paddingDp = 16.dp

    /*Surface(modifier = Modifier.padding(16.dp).fillMaxWidth(), color = Color.White, elevation = 6.dp, shape = RoundedCornerShape(6.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(paddingDp)) {
                Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = stringResource(R.string.musicsSortOrder), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                space(paddingDp)

                AudiosSortOrder.values().forEach {item ->
                    val spaceDp = 10.dp

                    Column(modifier = Modifier.fillMaxWidth().clickable { onItemClick(item) }) {
                        Divider(color = Color.LightGray, thickness = 0.6.dp)
                        space(spaceDp)
                        Text(textAlign = TextAlign.Center, text = stringResource(item.textResource), color = if (focusedItem == item) Color.Blue else Color.DarkGray)
                        space(spaceDp)
                    }
                }
            }
        }*/
}

@Composable
fun NoContent() {

}

@Composable
fun ChartProgressBar(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = "Loading..."
    )
}

/**if musicsList is null, it means it is loading*/
@Composable
fun Content(
    modifier: Modifier = Modifier,
    musicsList: List<Long>? = null,
    onPlayMusic: (Long) -> Unit,
    onGetDisplayNameById: (Long) -> String,
    onGetAlbumNameById: (Long) -> String,
    onGetDataById: (Long) -> String
) {
    if (musicsList == null)
        ChartProgressBar(modifier = modifier)
    else {

        if (musicsList.isNotEmpty())
            MusicsList(
                musicsList = musicsList,
                onGetDisplayNameById = onGetDisplayNameById,
                onGetAlbumNameById = onGetAlbumNameById,
                onGetDataById = onGetDataById,
                onPlayMusic = onPlayMusic
            )
        else
            NoContent()
    }
}

@Composable
fun MusicsListItem(
    displayName: String,
    albumName: String,
    musicData: String,
    onOpenOptionsDialog: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onOpenOptionsDialog) {
            Icon(
                modifier = Modifier.scale(0.8f),
                painter = painterResource(R.drawable.options_menu),
                tint = Color.DarkGray.copy(alpha = 0.8f)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = displayName,
                textAlign = TextAlign.End,
                fontSize = 14.sp,
                color = Color.Black.copy(alpha = 0.9f)
            )
            Text(
                modifier = Modifier.padding(end = 10.dp, top = 3.dp),
                textAlign = TextAlign.End,
                text = albumName,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        val bitmapCacheHelper by rememberUpdatedState(BitmapCacheHelper())

        val thumbnail by produceState<Bitmap?>(initialValue = null, musicData) {
            value = run {
                bitmapCacheHelper.get(musicData) ?: run {
                    musicData.musicThumbnail.also {bmp ->
                        bmp?.let { bitmapCacheHelper.put(musicData, it) }
                    }
                }
            }
        }

        MusicsListItemImage(
            thumbnail = thumbnail
        )
    }
}

@Composable
fun MusicsListItemImage(thumbnail: Bitmap?) {
    val modifier = Modifier.size(45.dp)

    if (thumbnail == null)
        Image(painter = painterResource(R.drawable.music_logo), modifier = modifier)
    else
        Image(bitmap = thumbnail.asImageBitmap(), modifier = modifier)
}

@Composable
fun MusicsList(
    musicsList: List<Long>,
    onGetDisplayNameById: (Long) -> String,
    onGetAlbumNameById: (Long) -> String,
    onGetDataById: (Long) -> String,
    onPlayMusic: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = SearchBarHeight + ContentsTopBarHeight + DividerHeight + PanelHeight + lineDividerHeight)
    ) {
        items(musicsList.size) { musicIndex ->
            val audioId = musicsList[musicIndex]

            Column {

                val displayName by produceState(initialValue = "") {
                    value = onGetDisplayNameById(audioId)
                }

                val musicData by produceState(initialValue = "", audioId) {
                    value = onGetDataById(audioId)
                }

                val musicAlbumName by produceState(initialValue = "", audioId) {
                    value = onGetAlbumNameById(audioId)
                }

                MusicsListItem(
                    displayName = displayName,
                    albumName = musicAlbumName,
                    musicData = musicData,
                    onOpenOptionsDialog = { },
                    onClick = {
                        //play the clicked music
                        onPlayMusic(audioId)
                    }
                )

                if (musicIndex < musicsList.size - 1)
                    Divider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 0.6.dp)
            }
        }
    }
}

@Composable
fun Panel(
    offset: Int,
    onAppsClick: () -> Unit,
    onSwapClick: () -> Unit,
    onCheckAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .offsetY(offset)
            .fillMaxWidth()
            .height(PanelHeight)
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {

        val iconTint = Color.Gray
        val icon: @Composable (Int) -> Unit = { resource ->
            val iconScale = when (resource) {
                R.drawable.shuffle -> 0.72f
                R.drawable.apps -> 0.603f
                else -> 0.9f
            }
            val iconRotation = when (resource) {
                R.drawable.swap -> 90f
                else -> 0f
            }
            Icon(
                modifier = Modifier
                    .size(35.dp)
                    .scale(iconScale)
                    .rotate(iconRotation)
                    .clickable {
                        when (resource) {

                            R.drawable.apps -> onAppsClick()
                            R.drawable.swap -> onSwapClick()
                            R.drawable.checkall -> onCheckAllClick()
                            R.drawable.shuffle -> onShuffleClick()
                        }
                    }, painter = painterResource(resource), tint = iconTint
            )
        }
        val spacerWidth = 8.dp

        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconsList = listOf(R.drawable.apps, R.drawable.swap, R.drawable.checkall)
            iconsList.forEachIndexed { index, iconResource ->
                icon(iconResource)
                if (index < iconsList.size - 1)
                    Spacer(
                        modifier = Modifier
                            .width(spacerWidth)
                            .fillMaxHeight()
                    )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.shuffle), fontSize = 15.sp)
            Spacer(
                modifier = Modifier
                    .width(spacerWidth)
                    .fillMaxHeight()
            )
            icon(R.drawable.shuffle)
        }
    }
}

@Composable
fun SearchBar(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SearchBarHeight)
            .background(Color.White)
            .padding(vertical = 6.dp, horizontal = 14.dp)
            .background(Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(7.dp)
            .clickable { onClick() }
    ) {

        Icon(
            imageVector = Icons.Filled.Search, modifier = Modifier
                .align(Alignment.CenterStart)
                .alpha(0.9f)
        )
        Icon(
            imageVector = Icons.Filled.Menu, modifier = Modifier
                .align(Alignment.CenterEnd)
                .alpha(0.9f)
        )
    }
}

enum class ContentsTopBarItem(val titleResource: Int, val icon: Any) {
    PlayLists(R.string.playlists, R.drawable.playlists),
    FavoriteItems(R.string.favoriteItems, Icons.Filled.Favorite),
    RecentItems(R.string.recentItems, R.drawable.recent_items)
}

@Composable
fun ContentsTopBarItem(item: ContentsTopBarItem) {
    Column(
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val modifier = Modifier.size(22.dp)
        val iconTint = Color.DarkGray

        if (item.icon is ImageVector)
            Icon(modifier = modifier, imageVector = item.icon, tint = iconTint)
        else
            Icon(modifier = modifier, painter = painterResource(item.icon as Int), tint = iconTint)

        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = stringResource(item.titleResource),
            color = Color.Gray,
            fontSize = 13.sp
        )
    }
}

@Composable
fun ContentsTopBar(offset: Int) {
    Row(
        modifier = Modifier
            .offsetY(offset)
            .fillMaxWidth()
            .height(ContentsTopBarHeight)
            .background(Color.White)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {

        ContentsTopBarItem.values().forEach {
            ContentsTopBarItem(item = it)
        }
    }
}

@Composable
fun Icon(
    modifier: Modifier = Modifier,
    painter: Painter,
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    contentDescription: String? = null
) {
    Icon(
        modifier = modifier,
        painter = painter,
        contentDescription = contentDescription,
        tint = tint
    )
}

@Composable
fun Icon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    imageVector: ImageVector,
    contentDescription: String? = null
) {
    Icon(
        modifier = modifier,
        tint = tint,
        imageVector = imageVector,
        contentDescription = contentDescription
    )
}

fun Modifier.offsetY(offsetY: Int) = this.then(
    Modifier.offset { IntOffset(0, offsetY) }
)

@Composable
fun Image(painter: Painter, contentDescription: String? = null, modifier: Modifier = Modifier) {
    Image(painter = painter, contentDescription = contentDescription, modifier = modifier)
}

@Composable
fun Image(
    bitmap: ImageBitmap,
    contentScale: ContentScale = ContentScale.FillBounds,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Image(bitmap = bitmap, contentDescription = contentDescription, modifier = modifier)
}

val Float.isPositive
    get() = this > 0

val String.encode
    get() = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

val String?.musicThumbnail: Bitmap?
    get() {
        try {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(this)
            val thumbnailByteArray = mediaMetadataRetriever.embeddedPicture

            return BitmapFactory.decodeByteArray(
                thumbnailByteArray,
                0,
                thumbnailByteArray?.size ?: 0
            )

        } catch (e: Exception) {
            return null
        }
    }