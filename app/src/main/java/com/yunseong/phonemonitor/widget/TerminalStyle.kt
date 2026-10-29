package com.yunseong.phonemonitor.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.yunseong.phonemonitor.R
import com.yunseong.phonemonitor.data.DeviceStats
import com.yunseong.phonemonitor.data.formatKhz
import com.yunseong.phonemonitor.data.formatRate
import com.yunseong.phonemonitor.data.formatTemp
import com.yunseong.phonemonitor.data.thermalLabel

private val bg: GlanceModifier.() -> GlanceModifier = { background(ImageProvider(R.drawable.widget_terminal_bg)) }
private val gray = WColors.fixed(Color(0xFF7D8590))
private val text = WColors.fixed(Color(0xFFE6EDF3))
private val green = WColors.fixed(Color(0xFF3FB950))
private val purple = WColors.fixed(Color(0xFFBC8CFF))
private val orange = WColors.fixed(Color(0xFFF0883E))
private val cyan = WColors.fixed(Color(0xFF58A6FF))
private val buttonBg = WColors.fixed(Color(0xFF21262D))

/** 터미널: 검은 배경에 고정폭 글자와 ███░░ 막대. 낮·밤 모두 어두운 화면 */
@Composable
fun TerminalWidget(s: DeviceStats, live: Boolean) {
    val c = LocalContext.current
    val temp = s.battery.temperatureC?.let(::formatTemp) ?: "-"
    when (currentLayout()) {
        WidgetLayout.TINY -> WidgetFrame(bg, padding = 8.dp, center = true) {
            Mono("BAT", gray, 10.sp)
            Mono("${s.battery.percent}%", green, 16.sp, bold = true)
        }
        WidgetLayout.COMPACT -> WidgetFrame(bg, padding = 12.dp, center = true) {
            BarLine("BAT", s.battery.percent, 5, green)
            BarLine("RAM", s.memory.usedPercent, 5, purple)
        }
        WidgetLayout.SQUARE -> WidgetFrame(bg) {
            Prompt(s, live, "$ mon")
            Spacer(GlanceModifier.height(6.dp))
            Column {
                BarLine("BAT", s.battery.percent, 6, green)
                BarLine("RAM", s.memory.usedPercent, 6, purple)
                BarLine("STO", s.storage.usedPercent, 6, orange)
                KeyValue("TMP", temp)
                if (isTall()) KeyValue("HOT", c.getString(thermalLabel(s.thermal.status)))
            }
        }
        WidgetLayout.WIDE -> WidgetFrame(bg, padding = 12.dp, center = true) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    BarLine("BAT", s.battery.percent, 10, green)
                    BarLine("RAM", s.memory.usedPercent, 10, purple)
                }
                Column {
                    KeyValue("STO", "${s.storage.usedPercent}%")
                    KeyValue("TMP", temp)
                }
            }
        }
        WidgetLayout.LARGE -> WidgetFrame(bg) {
            Prompt(s, live)
            Spacer(GlanceModifier.height(6.dp))
            Column(GlanceModifier.fillMaxWidth()) {
                BarLine("BAT", s.battery.percent, 16, green)
                BarLine("RAM", s.memory.usedPercent, 16, purple)
                BarLine("STO", s.storage.usedPercent, 16, orange)
                Row {
                    KeyValue("TMP", temp)
                    Spacer(GlanceModifier.width(12.dp))
                    KeyValue("CPU", s.cpu.fastestKhz?.let(::formatKhz) ?: "-")
                }
                if (isTall()) {
                    KeyValue("HOT", c.getString(thermalLabel(s.thermal.status)))
                    KeyValue("NET", "↓ " + (formatRate(s.network.rxBytesPerSec) ?: "-") + "  ↑ " + (formatRate(s.network.txBytesPerSec) ?: "-"))
                }
            }
        }
    }
}

@Composable
private fun Prompt(s: DeviceStats, live: Boolean, command: String = "$ phonemon") {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Mono(command, cyan, 11.sp, modifier = GlanceModifier.defaultWeight())
        Mono(if (live) "● live" else timeText(s), if (live) green else gray, 10.sp)
        Spacer(GlanceModifier.width(8.dp))
        RefreshButton(buttonBg, text, size = 24.dp)
    }
}

@Composable
private fun BarLine(key: String, percent: Int, width: Int, color: ColorProvider) {
    Row {
        Mono("$key ", gray, 11.sp)
        Mono(asciiBar(percent, width), color, 11.sp)
        Mono(" ${percent.toString().padStart(3)}%", text, 11.sp)
    }
}

@Composable
private fun KeyValue(key: String, value: String) {
    Row {
        Mono("$key ", gray, 11.sp)
        Mono(value, text, 11.sp)
    }
}

@Composable
private fun Mono(value: String, color: ColorProvider, size: TextUnit, bold: Boolean = false, modifier: GlanceModifier = GlanceModifier) =
    Text(
        value,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = size, fontFamily = FontFamily.Monospace, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal),
        maxLines = 1,
    )
