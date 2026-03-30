package com.example.recepy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color
import com.example.recepy.data.preferences.AppTheme

private fun getColorScheme(
    darkTheme: Boolean,
    appTheme: AppTheme
) = when (appTheme) {
    AppTheme.ORANGE -> if (darkTheme) {
        darkColorScheme(
            primary = OrangePrimary,
            secondary = OrangeSecondary,
            tertiary = OrangeTertiary,
            background = BackgroundDark,
            surface = SurfaceDark
        )
    } else {
        lightColorScheme(
            primary = OrangePrimary,
            secondary = OrangeSecondary,
            tertiary = OrangeTertiary,
            background = BackgroundLight,
            surface = SurfaceLight
        )
    }
    AppTheme.BLUE -> if (darkTheme) {
        darkColorScheme(
            primary = BluePrimary,
            secondary = BlueSecondary,
            tertiary = BlueTertiary,
            background = BackgroundDark,
            surface = SurfaceDark
        )
    } else {
        lightColorScheme(
            primary = BluePrimary,
            secondary = BlueSecondary,
            tertiary = BlueTertiary,
            background = Color(0xFFF0F7FF),
            surface = Color(0xFFFBFCFF)
        )
    }
    AppTheme.GREEN -> if (darkTheme) {
        darkColorScheme(
            primary = GreenPrimary,
            secondary = GreenSecondary,
            tertiary = GreenTertiary,
            background = BackgroundDark,
            surface = SurfaceDark
        )
    } else {
        lightColorScheme(
            primary = GreenPrimary,
            secondary = GreenSecondary,
            tertiary = GreenTertiary,
            background = Color(0xFFF2F9F2),
            surface = Color(0xFFFBFFFB)
        )
    }
    AppTheme.PINK -> if (darkTheme) {
        darkColorScheme(
            primary = PinkPrimary,
            secondary = PinkSecondary,
            tertiary = PinkTertiary,
            background = BackgroundDark,
            surface = SurfaceDark
        )
    } else {
        lightColorScheme(
            primary = PinkPrimary,
            secondary = PinkSecondary,
            tertiary = PinkTertiary,
            background = Color(0xFFFFF2F5),
            surface = Color(0xFFFFFBFC)
        )
    }
    AppTheme.PURPLE -> if (darkTheme) {
        darkColorScheme(
            primary = PurplePrimary,
            secondary = PurpleSecondary,
            tertiary = PurpleTertiary,
            background = BackgroundDark,
            surface = SurfaceDark
        )
    } else {
        lightColorScheme(
            primary = PurplePrimary,
            secondary = PurpleSecondary,
            tertiary = PurpleTertiary,
            background = Color(0xFFF7F2F9),
            surface = Color(0xFFFCFBFF)
        )
    }
}

@Composable
fun RecepyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    appTheme: AppTheme = AppTheme.ORANGE,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getColorScheme(darkTheme, appTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
