package com.example.swolescroll.ui.theme

import SwoleTypography
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


// Define how our colors map to Material Design slots
private val SwoleColorScheme = lightColorScheme(
    primary = IronBlue,
    onPrimary = Color.White, // Text on buttons

    primaryContainer = ParchmentDark, // Top Bar background
    onPrimaryContainer = InkBlack,    // Text on Top Bar

    background = ParchmentLight,
    onBackground = InkBlack,

    surface = ParchmentLight,
    onSurface = InkBlack,

    surfaceVariant = ParchmentDark, // Card background color
    onSurfaceVariant = InkBlack,

    secondary = SwoleGold,
    error = MuscleRed
)

@Composable
fun SwoleScrollTheme(
    // We ignore dark mode for now to force the "Scroll" look 24/7!
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color = false means "Don't let Android change my colors based on my wallpaper"
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // If you want to support Android 12+ wallpaper colors, keep this.
        // But I set dynamicColor = false above to FORCE your Parchment look.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // For now, we use the same scheme for light/dark to keep the "Scroll" aesthetic consistent
        else -> SwoleColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SwoleTypography, // Uses the default type for now
        content = content
    )
}
