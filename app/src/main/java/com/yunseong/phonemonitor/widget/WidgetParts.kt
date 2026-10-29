package com.yunseong.phonemonitor.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.yunseong.phonemonitor.MainActivity
import com.yunseong.phonemonitor.R
import com.yunseong.phonemonitor.data.DeviceStats
import java.text.DateFormat
import java.util.Date

/** 위젯 색. 앱 화면(ui/Theme.kt Accent)과 같은 계열 */
object WColors {
    val background = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF16181F))
    val onBackground = ColorProvider(day = Color(0xFF14151A), night = Color(0xFFF2F3F7))
    val muted = ColorProvider(day = Color(0xFF6B7080), night = Color(0xFF8B90A0))
    val track = ColorProvider(day = Color(0xFFE6E8F0), night = Color(0xFF262A35))
    val battery = ColorProvider(day = Color(0xFF10B981), night = Color(0xFF34D399))
    val memory = ColorProvider(day = Color(0xFF6366F1), night = Color(0xFFA78BFA))
    val storage = ColorProvider(day = Color(0xFFF97316), night = Color(0xFFFBBF24))
    val warn = ColorProvider(day = Color(0xFFD97706), night = Color(0xFFF59E0B))
    val danger = ColorProvider(day = Color(0xFFDC2626), night = Color(0xFFEF4444))

    /** 링 그림(비트맵)용. 낮·밤 모두에서 보이는 색 */
    val batteryRing = Color(0xFF34D399) to Color(0xFF14B8A6)
    val memoryRing = Color(0xFFA78BFA) to Color(0xFF6366F1)
    val storageRing = Color(0xFFFBBF24) to Color(0xFFF97316)
    val ringTrack = Color(0x40808080)

    fun fixed(color: Color) = ColorProvider(day = color, night = color)

    /** 배터리 잔량에 따라 초록·주황·빨강 */
    fun batteryLevel(percent: Int) = when {
        percent <= 15 -> danger
        percent <= 35 -> warn
        else -> battery
    }
}

/** 모든 위젯의 바깥 틀. 누르면 앱이 열린다 */
@Composable
fun WidgetFrame(
    background: GlanceModifier.() -> GlanceModifier = { background(WColors.background) },
    padding: Dp = 14.dp,
    center: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalAlignment = if (center) Alignment.CenterVertically else Alignment.Top,
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start,
        modifier =
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background()
            .padding(padding)
            .clickable(actionStartActivity<MainActivity>()),
        content = content,
    )
}

fun timeText(s: DeviceStats): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(s.readAt))

/** 위쪽 줄: 실시간이면 LIVE, 아니면 갱신 시각 + 새로 고침 버튼 */
@Composable
fun WidgetHeader(
    s: DeviceStats,
    live: Boolean,
    textColor: ColorProvider = WColors.muted,
    buttonBg: ColorProvider = WColors.track,
    iconColor: ColorProvider = WColors.onBackground,
    liveColor: ColorProvider = WColors.battery,
    title: String? = null,
) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            listOfNotNull(title, if (live) "● LIVE" else timeText(s)).joinToString("  "),
            style = TextStyle(color = if (live) liveColor else textColor, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
        )
        RefreshButton(buttonBg, iconColor)
    }
}

@Composable
fun RefreshButton(bg: ColorProvider, tint: ColorProvider, size: Dp = 28.dp) {
    val context = LocalContext.current
    Box(
        GlanceModifier.size(size).cornerRadius(size / 2).background(bg).clickable(actionRunCallback<RefreshAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            ImageProvider(R.drawable.ic_refresh),
            contentDescription = context.getString(R.string.refresh),
            colorFilter = ColorFilter.tint(tint),
            modifier = GlanceModifier.size(size * 0.57f),
        )
    }
}

@Composable
fun Meter(percent: Int, color: ColorProvider, track: ColorProvider = WColors.track, height: Dp = 6.dp) {
    LinearProgressIndicator(
        progress = percent / 100f,
        modifier = GlanceModifier.fillMaxWidth().height(height).cornerRadius(height / 2),
        color = color,
        backgroundColor = track,
    )
}

@Composable
fun Label(text: String, color: ColorProvider = WColors.muted, size: TextUnit = 11.sp) =
    Text(text, style = TextStyle(color = color, fontSize = size), maxLines = 1)

@Composable
fun Value(text: String, color: ColorProvider = WColors.onBackground, size: TextUnit = 14.sp, weight: FontWeight = FontWeight.Bold) =
    Text(text, style = TextStyle(color = color, fontSize = size, fontWeight = weight), maxLines = 1)

/** 270도 링을 그림으로 만든다. Glance는 호를 그릴 수 없어서 비트맵으로 넣는다 */
fun ringBitmap(context: Context, percent: Int, colors: Pair<Color, Color>, sizeDp: Float, strokeDp: Float): Bitmap {
    val density = context.resources.displayMetrics.density
    val px = (sizeDp * density).toInt().coerceAtLeast(1)
    val stroke = strokeDp * density
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val rect = RectF(stroke / 2, stroke / 2, px - stroke / 2, px - stroke / 2)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }
    paint.color = WColors.ringTrack.toArgb()
    canvas.drawArc(rect, 135f, 270f, false, paint)
    val sweep = 270f * percent.coerceIn(0, 100) / 100f
    if (sweep > 0f) {
        // 셰이더를 써도 paint 색의 투명도가 곱해진다. 트랙 색을 지우고 불투명하게
        paint.color = android.graphics.Color.BLACK
        paint.shader = SweepGradient(px / 2f, px / 2f, intArrayOf(colors.first.toArgb(), colors.second.toArgb(), colors.first.toArgb()), null)
            .apply { setLocalMatrix(Matrix().apply { setRotate(135f, px / 2f, px / 2f) }) }
        canvas.drawArc(rect, 135f, sweep, false, paint)
    }
    return bitmap
}

/** 링 + 가운데 숫자 */
@Composable
fun Ring(percent: Int, colors: Pair<Color, Color>, size: Dp, stroke: Dp, textSize: TextUnit, label: String? = null) {
    val context = LocalContext.current
    Box(GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        Image(ImageProvider(ringBitmap(context, percent, colors, size.value, stroke.value)), contentDescription = null, modifier = GlanceModifier.size(size))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Value("$percent%", size = textSize)
            if (label != null) Label(label, size = 9.sp)
        }
    }
}

/** 터미널 모양 막대: ███░░ */
fun asciiBar(percent: Int, width: Int): String {
    val filled = (percent.coerceIn(0, 100) * width + 50) / 100
    return "█".repeat(filled) + "░".repeat(width - filled)
}
