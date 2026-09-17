package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = Color(0xFF00390B),
    primaryContainer = EmeraldDarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFB7F397),
    secondary = WhatsAppTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF074E45),
    onSecondaryContainer = Color(0xFF70E3D1),
    tertiary = AccentCoral,
    background = SurfaceDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceContainerDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = Color(0xFF282D28),
    surfaceContainerHighest = Color(0xFF333833),
    onBackground = Color(0xFFE2E3DE),
    onSurface = Color(0xFFE2E3DE),
    onSurfaceVariant = Color(0xFFC2C8C1),
    outline = Color(0xFF8C938B),
    outlineVariant = Color(0xFF424942),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = WhatsAppTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = AccentCoral,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceContainerLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = Color(0xFFE0E5DF),
    surfaceContainerHighest = Color(0xFFD6DBD5),
    onBackground = Color(0xFF191C19),
    onSurface = Color(0xFF191C19),
    onSurfaceVariant = Color(0xFF424942),
    outline = Color(0xFF727972),
    outlineVariant = Color(0xFFC2C8C1),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Use our handcrafted WhatsApp/Emerald styling
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

