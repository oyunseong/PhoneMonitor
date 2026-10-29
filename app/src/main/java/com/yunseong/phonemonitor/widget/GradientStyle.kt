package com.yunseong.phonemonitor.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import com.yunseong.phonemonitor.R
import com.yunseong.phonemonitor.data.DeviceStats
import com.yunseong.phonemonitor.data.batteryStatusLabel
import com.yunseong.phonemonitor.data.formatTemp

private val white = WColors.fixed(Color.White)
private val whiteMuted = WColors.fixed(Color(0xC8FFFFFF))
private val whiteTrack = WColors.fixed(Color(0x40FFFFFF))
private val gradient: GlanceModifier.() -> GlanceModifier = { background(ImageProvider(R.drawable.widget_gradient)) }

/** 그라데이션: 보라→남색→청록 배경에 흰 글자 */
@Composable
fun GradientWidget(s: DeviceStats, live: Boolean) {
    val c = LocalContext.current
    val battery = c.getString(R.string.card_battery)
    val storage = c.getString(R.string.short_storage)
    val temp = s.battery.temperatureC?.let(::formatTemp)
    val header = @Composable {
        WidgetHeader(s, live, textColor = whiteMuted, buttonBg = whiteTrack, iconColor = white, liveColor = white)
    }
    when (currentLayout()) {
        WidgetLayout.TINY -> WidgetFrame(gradient, padding = 8.dp, center = true) {
            Value("${s.battery.percent}%", color = white, size = 20.sp)
            Label(battery, color = whiteMuted, size = 10.sp)
        }
        WidgetLayout.COMPACT -> WidgetFrame(gradient, padding = 12.dp, center = true) {
            Column(GlanceModifier.fillMaxWidth()) {
                Value("${s.battery.percent}%", color = white, size = 22.sp)
                Label(listOfNotNull(battery, temp).joinToString(" · "), color = whiteMuted)
            }
        }
        WidgetLayout.SQUARE -> WidgetFrame(gradient) {
            header()
            Value("${s.battery.percent}%", color = white, size = if (isTall()) 40.sp else 28.sp)
            Label(listOfNotNull(c.getString(batteryStatusLabel(s.battery.status)), temp).joinToString(" · "), color = whiteMuted)
            Spacer(GlanceModifier.height(if (isTall()) 12.dp else 4.dp))
            Meter(s.battery.percent, white, whiteTrack)
            Spacer(GlanceModifier.height(6.dp))
            Label("RAM ${s.memory.usedPercent}%   $storage ${s.storage.usedPercent}%", color = whiteMuted)
        }
        WidgetLayout.WIDE -> WidgetFrame(gradient, padding = 12.dp, center = true) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Number(battery, s.battery.percent, 22, bar = false)
                Number("RAM", s.memory.usedPercent, 22, bar = false)
                Number(storage, s.storage.usedPercent, 22, bar = false)
                RefreshButton(whiteTrack, white)
            }
        }
        WidgetLayout.LARGE -> WidgetFrame(gradient) {
            header()
            Spacer(GlanceModifier.height(if (isTall()) 12.dp else 4.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                Number(battery, s.battery.percent, if (isTall()) 34 else 26, bar = true)
                Number("RAM", s.memory.usedPercent, if (isTall()) 34 else 26, bar = true)
                Number(storage, s.storage.usedPercent, if (isTall()) 34 else 26, bar = true)
            }
            Spacer(GlanceModifier.height(if (isTall()) 14.dp else 6.dp))
            Label(infoLine(s), color = whiteMuted)
        }
    }
}

@Composable
private fun RowScope.Number(label: String, percent: Int, sizeSp: Int, bar: Boolean) {
    Column(GlanceModifier.defaultWeight().padding(end = 12.dp)) {
        Label(label, color = whiteMuted, size = 10.sp)
        Value("$percent%", color = white, size = sizeSp.sp)
        if (bar) {
            Spacer(GlanceModifier.height(4.dp))
            Meter(percent, white, whiteTrack, height = 4.dp)
        }
    }
}
