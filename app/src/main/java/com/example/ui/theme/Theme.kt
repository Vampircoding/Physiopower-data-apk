package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PolishPrimaryBlueDark,
    onPrimary = PolishOnPrimaryContainer,
    primaryContainer = PolishPrimaryContainerDark,
    onPrimaryContainer = PolishOnPrimaryContainerDark,
    secondary = PolishPurpleContainerDark,
    onSecondary = PolishTextLight,
    tertiary = PolishGreen,
    background = PolishBgDark,
    onBackground = PolishTextLight,
    surface = PolishSurfaceDark,
    onSurface = PolishTextLight,
    surfaceVariant = PolishSurfaceVariantDark,
    onSurfaceVariant = PolishTextLight,
    outline = PolishBorderDark,
    error = PolishRed
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PolishPrimaryBlue,
    onPrimary = PolishSurfaceLight,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    secondary = PolishPurpleContainer,
    onSecondary = PolishTextDark,
    tertiary = PolishGreen,
    background = PolishBgLight,
    onBackground = PolishTextDark,
    surface = PolishSurfaceLight,
    onSurface = PolishTextDark,
    surfaceVariant = PolishSurfaceVariant,
    onSurfaceVariant = PolishGray,
    outline = PolishBorder,
    error = PolishRed
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Force custom brand color scheme
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
