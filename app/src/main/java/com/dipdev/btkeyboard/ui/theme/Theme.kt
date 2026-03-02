package com.dipdev.btkeyboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

val BTKeyboardColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = NavyDeep,
    primaryContainer = CyanDark,
    onPrimaryContainer = TextPrimary,

    secondary = Cyan300,
    onSecondary = NavyDeep,
    secondaryContainer = NavyElevated,
    onSecondaryContainer = TextPrimary,

    tertiary = SuccessGreen,
    onTertiary = NavyDeep,

    background = NavyDeep,
    onBackground = TextPrimary,

    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavyElevated,
    onSurfaceVariant = TextSecondary,

    outline = NavyBorder,
    outlineVariant = TextDisabled,

    error = ErrorRed,
    onError = TextPrimary,
    errorContainer = ErrorDark,
    onErrorContainer = TextPrimary,

    inverseSurface = TextPrimary,
    inverseOnSurface = NavyDeep,
    inversePrimary = CyanDark,
)

@Composable
fun BTKeyboardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BTKeyboardColorScheme,
        typography = AppTypography,
        content = content
    )
}
