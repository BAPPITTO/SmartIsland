/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

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

// High-contrast, clean Black & White palette accented with #D84315 (Deep Orange 800) and its warm variations
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD84315),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDE6),
    onPrimaryContainer = Color(0xFFBF360C),
    secondary = Color(0xFFBF360C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCEBE6),
    onSecondaryContainer = Color(0xFF3E1204),
    tertiary = Color(0xFFE64A19),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0D6),
    onTertiaryContainer = Color(0xFF381003),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF121212),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFF5A5A5A),
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFEDEDED)
)

// Dark palette tuned for comfort: no pure black or pure white (less glare and halation), softer
// surfaces, and a muted coral accent instead of the saturated orange used on light.
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE0957C),
    onPrimary = Color(0xFF2E140B),
    primaryContainer = Color(0xFF45291F),
    onPrimaryContainer = Color(0xFFF2D3C8),
    secondary = Color(0xFFD6A08F),
    onSecondary = Color(0xFF2E140B),
    secondaryContainer = Color(0xFF3A2620),
    onSecondaryContainer = Color(0xFFEBCFC5),
    tertiary = Color(0xFFD9AE9E),
    onTertiary = Color(0xFF2E140B),
    tertiaryContainer = Color(0xFF33231E),
    onTertiaryContainer = Color(0xFFEBD3CA),
    background = Color(0xFF121214),
    onBackground = Color(0xFFE4E2E0),
    surface = Color(0xFF1C1C1F),
    onSurface = Color(0xFFE4E2E0),
    surfaceVariant = Color(0xFF28282C),
    onSurfaceVariant = Color(0xFFA9A6A3),
    outline = Color(0xFF3C3C41),
    outlineVariant = Color(0xFF2C2C30)
)

@Composable
fun SmartIslandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** "system" follows [darkTheme]; "light" / "dark" force one. */
    themeMode: String = "system",
    dynamicColor: Boolean = false, // Use our tailored premium palette by default for maximum brand consistency
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val useDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> darkTheme
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> DarkColorScheme
        else -> LightColorScheme
    }

    // Keep the status / navigation bar icons readable when the chosen theme differs from the phone's.
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (context as? android.app.Activity)?.window ?: return@SideEffect
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !useDark
            controller.isAppearanceLightNavigationBars = !useDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

