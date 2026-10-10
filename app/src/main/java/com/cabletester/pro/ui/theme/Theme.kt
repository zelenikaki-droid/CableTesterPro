package com.cabletester.pro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color // POPRAVEK: Dodan manjkajoči uvoz za Color

// Prenovljena svetla tema brez rdečih gumbov
private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,           // Gumb TEST bo indigo moder
    onPrimary = Color.White,           // Belo besedilo na indigo gumbu
    secondary = LightComponent,        // Nevtralni gumbi bodo svetlo sivi
    onSecondary = LightText,           // Temen tekst na sivih gumbih
    tertiary = LightStatusPass,        // Rezultat: PASS (Zelena)
    onTertiary = Color.White,
    background = LightBg,              // Svetlo sivo-belo ozadje zaslona
    onBackground = LightText,          // Temen tekst za splošna obvestila
    surface = LightSurface,            // Bele kartice profilov
    onSurface = LightText,
    surfaceVariant = LightComponent,
    onSurfaceVariant = LightText,
    error = LightStatusFail,           // Rezultat: FAIL (Rdeča)
    onError = Color.White
)

// Preverjena temna tema
private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,            // Gumb TEST bo električno moder
    onPrimary = DarkBg,                // Temen tekst na modrem gumbu
    secondary = DarkComponent,         // Nevtralni gumbi bodo srednje sivi
    onSecondary = DarkText,            // Svetel tekst na sivih gumbih
    tertiary = DarkStatusPass,         // Rezultat: PASS (Zelena)
    onTertiary = DarkText,
    background = DarkBg,               // Globoko temno ozadje zaslona
    onBackground = DarkText,           // Svetel tekst za splošna obvestila
    surface = DarkSurface,             // Temno sive kartice profilov
    onSurface = DarkText,
    surfaceVariant = DarkComponent,
    onSurfaceVariant = DarkText,
    error = DarkStatusFail,            // Rezultat: FAIL (Rdeča)
    onError = DarkText
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
