package com.cabletester.pro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = WhiteBackground,
    secondary = RedDark,
    onSecondary = WhiteBackground,
    tertiary = RedPrimary,
    background = WhiteBackground,
    onBackground = DarkText,
    surface = WhiteBackground,
    onSurface = DarkText,
    error = RedDark
)

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = DarkBg,
    secondary = DarkComponent,
    onSecondary = LightText,
    tertiary = StatusPass,
    onTertiary = LightText,
    background = DarkBg,
    onBackground = LightText,
    surface = DarkSurface,
    onSurface = LightText,
    surfaceVariant = DarkComponent,
    onSurfaceVariant = LightText,
    error = StatusFail,
    onError = LightText
)

@Composable
fun CableTesterProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
