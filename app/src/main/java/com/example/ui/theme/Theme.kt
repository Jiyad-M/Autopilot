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

private val DarkColorScheme = darkColorScheme(
  primary = NavPrimaryDark,
  onPrimary = NavOnPrimaryDark,
  primaryContainer = NavPrimaryContainerDark,
  onPrimaryContainer = NavOnPrimaryContainerDark,
  secondary = NavSecondaryDark,
  onSecondary = NavOnSecondaryDark,
  secondaryContainer = NavSecondaryContainerDark,
  onSecondaryContainer = NavOnSecondaryContainerDark,
  surface = NavSurfaceDark,
  onSurface = NavOnSurfaceDark,
  surfaceVariant = NavSurfaceVariantDark,
  onSurfaceVariant = NavOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
  primary = NavPrimaryLight,
  onPrimary = NavOnPrimaryLight,
  primaryContainer = NavPrimaryContainerLight,
  onPrimaryContainer = NavOnPrimaryContainerLight,
  secondary = NavSecondaryLight,
  onSecondary = NavOnSecondaryLight,
  secondaryContainer = NavSecondaryContainerLight,
  onSecondaryContainer = NavOnSecondaryContainerLight,
  surface = NavSurfaceLight,
  onSurface = NavOnSurfaceLight,
  surfaceVariant = NavSurfaceVariantLight,
  onSurfaceVariant = NavOnSurfaceVariantLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our crisp navigation colors by default
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
