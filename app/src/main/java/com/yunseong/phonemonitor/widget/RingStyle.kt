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
import androidx.glance.layout.width
import com.yunseong.phonemonitor.R
import com.yunseong.phonemonitor.data.DeviceStats

/** 링: 앱 첫 화면과 같은 270도 게이지 */
@Composable
fun RingWidget(s: DeviceStats, live: Boolean) {
    val c = LocalContext.current
    val battery = c.getString(R.string.card_battery)
    val storage = c.getString(R.string.short_storage)
    when (currentLayout()) {
        WidgetLayout.TINY -> WidgetFrame(padding = 6.dp, center = true) {
            Ring(s.battery.percent, WColors.batteryRing, 50.dp, 5.dp, 13.sp)
        }
        WidgetLayout.COMPACT -> WidgetFrame(padding = 10.dp, center = true) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Ring(s.battery.percent, WColors.batteryRing, 52.dp, 5.dp, 13.sp)
                Spacer(GlanceModifier.width(10.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Value("RAM ${s.memory.usedPercent}%", size = 12.sp)
                    Spacer(GlanceModifier.height(4.dp))
                    Value("$storage ${s.storage.usedPercent}%", size = 12.sp)
                }
            }
        }
        WidgetLayout.SQUARE -> WidgetFrame {
            WidgetHeader(s, live)
            Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                Ring(s.battery.percent, WColors.batteryRing, if (isTall()) 112.dp else 66.dp, if (isTall()) 9.dp else 6.dp, if (isTall()) 20.sp else 15.sp, if (isTall()) battery else null)
            }
            Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Label("RAM ${s.memory.usedPercent}%   $storage ${s.storage.usedPercent}%")
            }
        }
        WidgetLayout.WIDE -> WidgetFrame(padding = 8.dp, center = true) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RingCell(s.battery.percent, WColors.batteryRing, 48, null)
                RingCell(s.memory.usedPercent, WColors.memoryRing, 48, null)
                RingCell(s.storage.usedPercent, WColors.storageRing, 48, null)
            }
        }
        WidgetLayout.LARGE -> WidgetFrame {
            WidgetHeader(s, live)
            Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                // 낮으면 이름을 링 안에 넣어 높이를 줄인다
                val tall = isTall()
                val size = if (tall) 88 else 58
                RingCell(s.battery.percent, WColors.batteryRing, size, battery, inside = !tall)
                RingCell(s.memory.usedPercent, WColors.memoryRing, size, "RAM", inside = !tall)
                RingCell(s.storage.usedPercent, WColors.storageRing, size, storage, inside = !tall)
            }
            Label(infoLine(s))
        }
    }
}

@Composable
private fun RowScope.RingCell(
    percent: Int,
    colors: Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color>,
    sizeDp: Int,
    label: String?,
    inside: Boolean = false,
) {
    Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Ring(percent, colors, sizeDp.dp, (sizeDp / 10f).coerceAtLeast(4f).dp, if (sizeDp >= 68) 15.sp else 12.sp, if (inside) label else null)
            if (label != null && !inside) {
                Spacer(GlanceModifier.height(2.dp))
                Label(label, size = 10.sp)
            }
        }
    }
}
