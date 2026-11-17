package com.xpodcaster.xpodcaster.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val Teal = Color(0xFF35C6C0)
val OnTeal = Color(0xFF002A28)
val CardBg = Color(0xFF1B2226)
val Surface2 = Color(0xFF232C31)
val Muted = Color(0xFF9AA8B0)
val Danger = Color(0xFFFF6B5E)
val DangerBg = Color(0xFF3A1E1C)
val Body = Color(0xFFD5DDE1)

private val colors = Colors(
    primary = Teal,
    primaryVariant = Color(0xFF1F9E9A),
    secondary = Teal,
    onPrimary = OnTeal,
    onSecondary = OnTeal,
    surface = CardBg,
    onSurface = Color(0xFFEEF3F5),
    onSurfaceVariant = Muted,
    background = Color.Black,
    onBackground = Color(0xFFEEF3F5),
    error = Danger,
)

@Composable
fun WearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = colors, content = content)
}
