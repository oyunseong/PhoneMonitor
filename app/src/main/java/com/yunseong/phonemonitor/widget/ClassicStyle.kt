package com.yunseong.phonemonitor.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.yunseong.phonemonitor.R
import com.yunseong.phonemonitor.data.DeviceStats
import com.yunseong.phonemonitor.data.batteryStatusLabel
import com.yunseong.phonemonitor.data.formatKhz
import com.yunseong.phonemonitor.data.formatTemp
import com.yunseong.phonemonitor.data.thermalLabel

/** 클래식: 흰 카드에 막대 */
@Composable
fun ClassicWidget(s: DeviceStats, live: Boolean) {
    val c = LocalContext.current
    val battery = c.getString(R.string.card_battery)
    val storage = c.getString(R.string.short_storage)
    val temp = s.battery.temperatureC?.let(::formatTemp)
    when (currentLayout()) {
        WidgetLayout.TINY -> WidgetFrame(padding = 8.dp, center = true) {
            Value("${s.battery.percent}%", size = 20.sp)
            Label(battery, size = 10.sp)
        }
        WidgetLayout.COMPACT -> WidgetFrame(padding = 12.dp, center = true) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Value("${s.battery.percent}%", size = 22.sp)
                    Label(temp ?: battery)
                }
                Column(GlanceModifier.width(56.dp)) {
                    Label("RAM ${s.memory.usedPercent}%", size = 10.sp)
                    Spacer(GlanceModifier.height(4.dp))
                    Meter(s.memory.usedPercent, WColors.memory, height = 5.dp)
                }
            }
        }
        WidgetLayout.SQUARE -> WidgetFrame {
            WidgetHeader(s, live)
            Spacer(GlanceModifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Value("${s.battery.percent}%", size = 26.sp)
                Spacer(GlanceModifier.width(6.dp))
                Text(temp ?: "", style = TextStyle(color = WColors.muted, fontSize = 11.sp), modifier = GlanceModifier.padding(bottom = 5.dp))
            }
            Spacer(GlanceModifier.height(4.dp))
            Meter(s.battery.percent, WColors.battery)
            Spacer(GlanceModifier.height(8.dp))
            LabeledMeter("RAM ${s.memory.usedPercent}%", s.memory.usedPercent, WColors.memory)
            if (isTall()) {
                Spacer(GlanceModifier.height(8.dp))
                LabeledMeter("$storage ${s.storage.usedPercent}%", s.storage.usedPercent, WColors.storage)
            }
        }
        WidgetLayout.WIDE -> WidgetFrame(padding = 12.dp, center = true) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MiniColumn(battery, s.battery.percent, WColors.battery)
                MiniColumn("RAM", s.memory.usedPercent, WColors.memory)
                MiniColumn(storage, s.storage.usedPercent, WColors.storage)
                RefreshButton(WColors.track, WColors.onBackground)
            }
        }
        WidgetLayout.LARGE -> WidgetFrame {
            WidgetHeader(s, live)
            Spacer(GlanceModifier.height(4.dp))
            if (!isTall()) {
                Column(GlanceModifier.fillMaxWidth()) {
                    MeterRow(battery, s.battery.percent, WColors.battery)
                    Spacer(GlanceModifier.height(6.dp))
                    MeterRow("RAM", s.memory.usedPercent, WColors.memory)
                    Spacer(GlanceModifier.height(6.dp))
                    MeterRow(storage, s.storage.usedPercent, WColors.storage)
                    Spacer(GlanceModifier.height(6.dp))
                    Label(infoLine(s))
                }
            } else Column(GlanceModifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Value("${s.battery.percent}%", size = 30.sp)
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        battery + " · " + c.getString(batteryStatusLabel(s.battery.status)),
                        style = TextStyle(color = WColors.muted, fontSize = 11.sp),
                        modifier = GlanceModifier.padding(bottom = 6.dp),
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                Meter(s.battery.percent, WColors.battery)
                Spacer(GlanceModifier.height(8.dp))
                MeterRow("RAM", s.memory.usedPercent, WColors.memory)
                Spacer(GlanceModifier.height(8.dp))
                MeterRow(storage, s.storage.usedPercent, WColors.storage)
                Spacer(GlanceModifier.height(8.dp))
                Label(infoLine(s))
            }
        }
    }
}

/** 온도 · CPU · 발열 한 줄 */
@Composable
fun infoLine(s: DeviceStats): String {
    val c = LocalContext.current
    return listOfNotNull(
        s.battery.temperatureC?.let(::formatTemp),
        s.cpu.fastestKhz?.let { "CPU " + formatKhz(it) },
        c.getString(R.string.card_thermal) + " " + c.getString(thermalLabel(s.thermal.status)),
    ).joinToString("  ·  ")
}

@Composable
private fun LabeledMeter(label: String, percent: Int, color: ColorProvider) {
    Column(GlanceModifier.fillMaxWidth()) {
        Label(label)
        Spacer(GlanceModifier.height(4.dp))
        Meter(percent, color)
    }
}

@Composable
private fun MeterRow(label: String, percent: Int, color: ColorProvider) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TextStyle(color = WColors.muted, fontSize = 11.sp), modifier = GlanceModifier.width(64.dp), maxLines = 1)
        Box(GlanceModifier.defaultWeight()) { Meter(percent, color) }
        Spacer(GlanceModifier.width(8.dp))
        Text(
            "$percent%",
            style = TextStyle(color = WColors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
            modifier = GlanceModifier.width(40.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.MiniColumn(label: String, percent: Int, color: ColorProvider) {
    Column(GlanceModifier.defaultWeight().padding(end = 12.dp)) {
        Row(GlanceModifier.fillMaxWidth()) {
            Text(label, style = TextStyle(color = WColors.muted, fontSize = 10.sp), modifier = GlanceModifier.defaultWeight(), maxLines = 1)
            Value("$percent%", size = 11.sp)
        }
        Spacer(GlanceModifier.height(4.dp))
        Meter(percent, color, height = 5.dp)
    }
}
