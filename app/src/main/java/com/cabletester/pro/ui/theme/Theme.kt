package com.cabletester.pro.ui.theme

import androidx.compose.material3.MaterialTheme
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

@Composable
fun CableTesterProTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}