package com.yunseong.phonemonitor.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.yunseong.phonemonitor.R
import com.yunseong.phonemonitor.data.DeviceStats
import com.yunseong.phonemonitor.data.batteryStatusLabel
import com.yunseong.phonemonitor.data.formatTemp

/** 큰 숫자: 배터리 잔량을 크게, 잔량에 따라 초록·주황·빨강 */
@Composable
fun FocusWidget(s: DeviceStats, live: Boolean) {
    val c = LocalContext.current
    val battery = c.getString(R.string.card_battery)
    val storage = c.getString(R.string.short_storage)
    val status = c.getString(batteryStatusLabel(s.battery.status))
    val temp = s.battery.temperatureC?.let(::formatTemp)
    val level = WColors.batteryLevel(s.battery.percent)
    when (currentLayout()) {
        WidgetLayout.TINY -> WidgetFrame(padding = 6.dp, center = true) {
            Value("${s.battery.percent}", color = level, size = 26.sp)
        }
        WidgetLayout.COMPACT -> WidgetFrame(padding = 12.dp, center = true) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Value("${s.battery.percent}%", color = level, size = 26.sp)
                Spacer(GlanceModifier.width(8.dp))
                Column(GlanceModifier.defaultWeight()) {
                    // 좁아서 긴 상태 글자 대신 충전 중일 때만 표시
                    Label(if (s.battery.status == android.os.BatteryManager.BATTERY_STATUS_CHARGING) status else battery)
                    Label(temp ?: "")
                }
            }
        }
        WidgetLayout.SQUARE -> WidgetFrame {
            WidgetHeader(s, live)
            Column(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                Value("${s.battery.percent}%", color = level, size = 44.sp)
                Label(listOfNotNull(status, temp).joinToString(" · "))
            }
            if (isTall()) Label("RAM ${s.memory.usedPercent}%   $storage ${s.storage.usedPercent}%")
        }
        WidgetLayout.WIDE -> WidgetFrame(padding = 12.dp, center = true) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Big(battery, s.battery.percent, level)
                Big("RAM", s.memory.usedPercent, WColors.memory)
                Big(storage, s.storage.usedPercent, WColors.storage)
            }
        }
        WidgetLayout.LARGE -> WidgetFrame {
            WidgetHeader(s, live)
            Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Value("${s.battery.percent}%", color = level, size = if (isTall()) 60.sp else 48.sp)
                    Label("$battery · $status")
                }
                Column(GlanceModifier.width(110.dp)) {
                    SideRow("RAM", "${s.memory.usedPercent}%", WColors.memory)
                    Spacer(GlanceModifier.height(6.dp))
                    SideRow(storage, "${s.storage.usedPercent}%", WColors.storage)
                    Spacer(GlanceModifier.height(6.dp))
                    SideRow(c.getString(R.string.label_temperature), temp ?: "-", WColors.onBackground)
                }
            }
        }
    }
}

@Composable
private fun SideRow(label: String, value: String, color: ColorProvider) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TextStyle(color = WColors.muted, fontSize = 11.sp), modifier = GlanceModifier.defaultWeight(), maxLines = 1)
        Value(value, color = color, size = 15.sp)
    }
}

@Composable
private fun RowScope.Big(label: String, percent: Int, color: ColorProvider) {
    Column(GlanceModifier.defaultWeight().padding(end = 8.dp)) {
        Value("$percent%", color = color, size = 24.sp)
        Label(label, size = 10.sp)
    }
}
