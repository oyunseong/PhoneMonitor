package com.yunseong.phonemonitor.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import com.yunseong.phonemonitor.data.DeviceStats
import com.yunseong.phonemonitor.data.StatsStore
import com.yunseong.phonemonitor.live.LiveUpdateService

/** 위젯 모양. 위젯 고르기 화면에 각각 따로 나온다 */
enum class WidgetStyle { CLASSIC, RING, GRADIENT, TERMINAL, FOCUS }

/**
 * 크기별 배치. 런처 칸 기준 대략 1×1, 2×1, 2×2, 4×1, 4×2.
 * 그 사이 크기는 들어가는 것 중 가장 큰 배치를 쓴다.
 */
enum class WidgetLayout { TINY, COMPACT, SQUARE, WIDE, LARGE }

@Composable
fun currentLayout(): WidgetLayout {
    val size = LocalSize.current
    val wide = size.width >= 250.dp
    val tall = size.height >= 140.dp
    return when {
        wide && tall -> WidgetLayout.LARGE
        wide -> WidgetLayout.WIDE
        size.width >= 110.dp && tall -> WidgetLayout.SQUARE
        size.width >= 110.dp -> WidgetLayout.COMPACT
        else -> WidgetLayout.TINY
    }
}

/** 2칸 높이는 기기마다 140~200dp. 190dp 이상이면 항목을 더 보여 준다 */
@Composable
fun isTall() = LocalSize.current.height >= 190.dp

open class StatusWidget(private val style: WidgetStyle) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(57.dp, 57.dp), DpSize(110.dp, 57.dp), DpSize(110.dp, 140.dp), DpSize(110.dp, 190.dp),
            DpSize(250.dp, 57.dp), DpSize(250.dp, 140.dp), DpSize(250.dp, 190.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        StatsStore.refresh(context)
        provideContent {
            // 위젯이 떠 있는 동안에는 provideGlance가 다시 불리지 않으므로 값은 Flow로 받는다
            val stats by StatsStore.stats.collectAsState()
            val live by LiveUpdateService.running.collectAsState()
            val current = stats
            if (current != null) Render(style, current, live) else Loading()
        }
    }
}

@Composable
private fun Render(style: WidgetStyle, s: DeviceStats, live: Boolean) = when (style) {
    WidgetStyle.CLASSIC -> ClassicWidget(s, live)
    WidgetStyle.RING -> RingWidget(s, live)
    WidgetStyle.GRADIENT -> GradientWidget(s, live)
    WidgetStyle.TERMINAL -> TerminalWidget(s, live)
    WidgetStyle.FOCUS -> FocusWidget(s, live)
}

/** 아직 값을 한 번도 못 읽었을 때. 누르면 다시 읽는다 */
@Composable
private fun Loading() {
    WidgetFrame(center = true) {
        Label("…")
        RefreshButton(WColors.track, WColors.onBackground)
    }
}

class ClassicStatusWidget : StatusWidget(WidgetStyle.CLASSIC)
class RingStatusWidget : StatusWidget(WidgetStyle.RING)
class GradientStatusWidget : StatusWidget(WidgetStyle.GRADIENT)
class TerminalStatusWidget : StatusWidget(WidgetStyle.TERMINAL)
class FocusStatusWidget : StatusWidget(WidgetStyle.FOCUS)

class ClassicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClassicStatusWidget()
}
class RingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RingStatusWidget()
}
class GradientWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GradientStatusWidget()
}
class TerminalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TerminalStatusWidget()
}
class FocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusStatusWidget()
}

object Widgets {
    private val all = listOf(ClassicStatusWidget(), RingStatusWidget(), GradientStatusWidget(), TerminalStatusWidget(), FocusStatusWidget())

    /** 놓여 있지 않은 모양은 아무 일도 하지 않는다 */
    suspend fun updateAll(context: Context) = all.forEach { it.updateAll(context) }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        StatsStore.refresh(context)
        Widgets.updateAll(context)
    }
}
