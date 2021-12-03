package ir.avesta.musicplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*darkColors(
primary = Purple200,
primaryVariant = Purple700,
secondary = Teal200
)*/

/*lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200

    *//* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    *//*
)*/

data class MyColors(
    val white_blue: Color
)

val LocalMyColors = staticCompositionLocalOf<MyColors> {
    error("No colors provided")
}

private val DarkColorPalette = MyColors(
    white_blue = Color(0xFF0A417A)
)

private val LightColorPalette = MyColors(
    white_blue = Color.White
)


@Composable
fun MusicPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    CompositionLocalProvider(LocalMyColors provides colors, content = content)
}