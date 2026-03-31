package com.cashpilot.android.ui.theme

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

private val CashPilotGreen = Color(0xFF22C55E)
private val CashPilotDarkGreen = Color(0xFF16A34A)

private val DarkColorScheme = darkColorScheme(
    primary = CashPilotGreen,
    secondary = CashPilotDarkGreen,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
)

private val LightColorScheme = lightColorScheme(
    primary = CashPilotDarkGreen,
    secondary = CashPilotGreen,
)

@Composable
fun CashPilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        content = content,
    )
}
