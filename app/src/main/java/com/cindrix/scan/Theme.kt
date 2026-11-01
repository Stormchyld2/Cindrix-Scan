package com.cindrix.scan

import android.graphics.Typeface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

// Cindrix palette: charcoal, bone, blue.
val Charcoal = Color(0xFF15181B)
val Panel = Color(0xFF1F2428)
val PanelHigh = Color(0xFF2A3036)
val Bone = Color(0xFFE6E1D3)
val Muted = Color(0xFF9AA3AB)
val Blue = Color(0xFF4C8DF6)
val Danger = Color(0xFFE5534B)

val HeadingFont = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))

// Buttons stay rounded.
val Round = RoundedCornerShape(14.dp)

@Composable
fun CindrixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Blue,
            onPrimary = Color(0xFF07101F),
            secondary = Blue,
            background = Charcoal,
            onBackground = Bone,
            surface = Panel,
            onSurface = Bone,
            surfaceVariant = PanelHigh,
            onSurfaceVariant = Muted,
            outline = Color(0xFF3A424A),
            error = Danger,
        ),
        content = content,
    )
}
