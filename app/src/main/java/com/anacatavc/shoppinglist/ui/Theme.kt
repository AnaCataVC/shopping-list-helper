package com.anacatavc.shoppinglist.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.anacatavc.shoppinglist.R

enum class ThemeMode(@StringRes val label: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}

private const val PREFS = "settings"
private const val KEY_THEME = "theme"

fun loadThemeMode(context: Context): ThemeMode =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, null)
        ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM

fun saveThemeMode(context: Context, mode: ThemeMode) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_THEME, mode.name).apply()
}

// Brand palette taken from the app icon (lavender cart, pink wheels).
private val LightColors = lightColorScheme(
    primary = Color(0xFF7B5FD9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DDFB),
    onPrimaryContainer = Color(0xFF2B1A66),
    secondary = Color(0xFFC2448A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBDDEE),
    onSecondaryContainer = Color(0xFF3E0A28),
    background = Color(0xFFF5F1FC),
    onBackground = Color(0xFF1C1A22),
    surface = Color(0xFFF5F1FC),
    onSurface = Color(0xFF1C1A22),
    surfaceVariant = Color(0xFFE6E0F0),
    onSurfaceVariant = Color(0xFF48454F),
    error = Color(0xFFBA1A36),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCBBEFF),
    onPrimary = Color(0xFF33208A),
    primaryContainer = Color(0xFF4B38A6),
    onPrimaryContainer = Color(0xFFE6DDFB),
    secondary = Color(0xFFFFB0D6),
    onSecondary = Color(0xFF5C1140),
    secondaryContainer = Color(0xFF7A2A59),
    onSecondaryContainer = Color(0xFFFBDDEE),
    background = Color(0xFF15121C),
    onBackground = Color(0xFFE7E1EE),
    surface = Color(0xFF15121C),
    onSurface = Color(0xFFE7E1EE),
    surfaceVariant = Color(0xFF48454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFFFB3B8),
)

@Composable
fun AppTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
