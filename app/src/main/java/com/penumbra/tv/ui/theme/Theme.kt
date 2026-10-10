package com.penumbra.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

object Palette {
    val Background = Color(0xFF09090C)
    val Surface = Color(0xFF141419)
    val SurfaceFocused = Color(0xFF1F1F27)
    val Track = Color(0x1AFFFFFF)
    val Hairline = Color(0x14FFFFFF)
    val TextPrimary = Color(0xFFF2EEE9)
    val TextSecondary = Color(0xFF8F8C97)
    val TextTertiary = Color(0xFF5E5C66)
}

object Type {
    val Brand = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp)
    val Clock = TextStyle(
        fontSize = 96.sp,
        lineHeight = 104.sp,
        fontWeight = FontWeight.ExtraLight,
        letterSpacing = (-2).sp,
        fontFeatureSettings = "tnum",
    )
    val Date = TextStyle(fontSize = 17.sp, lineHeight = 24.sp)
    val StatusTitle = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium)
    val Body = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
    val Section = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.4.sp)
    val RowTitle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val RowSubtitle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val Value = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
    val Button = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
}

/** Accent colour of the selected filter tone; the whole interface follows it. */
val LocalAccent = compositionLocalOf { Color(0xFFFFA64A) }

@Composable
fun PenumbraTheme(accent: Color, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            onPrimary = Palette.Background,
            background = Palette.Background,
            onBackground = Palette.TextPrimary,
            surface = Palette.Surface,
            onSurface = Palette.TextPrimary,
        ),
    ) {
        CompositionLocalProvider(
            LocalAccent provides accent,
            LocalContentColor provides Palette.TextPrimary,
            content = content,
        )
    }
}
