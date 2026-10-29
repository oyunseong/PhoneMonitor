package com.yunseong.phonemonitor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 항목별 그라데이션. 앱과 위젯이 같은 색을 쓴다 */
object Accent {
    val battery = listOf(Color(0xFF34D399), Color(0xFF14B8A6))
    val memory = listOf(Color(0xFFA78BFA), Color(0xFF6366F1))
    val storage = listOf(Color(0xFFFBBF24), Color(0xFFF97316))
    val cpu = listOf(Color(0xFF38BDF8), Color(0xFF3B82F6))
    val network = listOf(Color(0xFFF472B6), Color(0xFFA855F7))
    val warn = Color(0xFFF59E0B)
    val danger = Color(0xFFEF4444)
}

@Immutable
data class Surfaces(
    val card: Color,
    val track: Color,
    val muted: Color,
)

val LocalSurfaces = staticCompositionLocalOf { Surfaces(Color.White, Color.LightGray, Color.Gray) }

private val darkScheme = darkColorScheme(
    primary = Color(0xFFA78BFA),
    background = Color(0xFF0B0C10),
    surface = Color(0xFF0B0C10),
    onBackground = Color(0xFFF2F3F7),
    onSurface = Color(0xFFF2F3F7),
)
private val lightScheme = lightColorScheme(
    primary = Color(0xFF6366F1),
    background = Color(0xFFF4F5F9),
    surface = Color(0xFFF4F5F9),
    onBackground = Color(0xFF14151A),
    onSurface = Color(0xFF14151A),
)

private val typography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

@Composable
fun PhoneMonitorTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val surfaces = if (dark) Surfaces(Color(0xFF16181F), Color(0xFF262A35), Color(0xFF8B90A0))
    else Surfaces(Color.White, Color(0xFFE6E8F0), Color(0xFF6B7080))
    val scheme = if (dark) darkScheme else lightScheme
    MaterialTheme(colorScheme = scheme, typography = typography) {
        // Surface 없이 그리므로 글자 기본색을 직접 정한다 (없으면 다크 모드에서 검은 글자)
        CompositionLocalProvider(LocalSurfaces provides surfaces, LocalContentColor provides scheme.onBackground, content = content)
    }
}
