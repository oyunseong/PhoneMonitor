package com.yunseong.phonemonitor.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.yunseong.phonemonitor.permission.AppPermission
import com.yunseong.phonemonitor.permission.rememberPermissionRequest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import com.yunseong.phonemonitor.R
import com.yunseong.phonemonitor.cleanup.Cleanup
import com.yunseong.phonemonitor.data.BatteryStats
import com.yunseong.phonemonitor.data.CpuStats
import com.yunseong.phonemonitor.data.DeviceStats
import com.yunseong.phonemonitor.data.ThermalStats
import com.yunseong.phonemonitor.data.batteryStatusLabel
import com.yunseong.phonemonitor.data.formatBytes
import com.yunseong.phonemonitor.data.formatKhz
import com.yunseong.phonemonitor.data.formatRate
import com.yunseong.phonemonitor.data.formatTemp
import com.yunseong.phonemonitor.data.healthLabel
import com.yunseong.phonemonitor.data.pluggedLabel
import com.yunseong.phonemonitor.data.thermalLabel
import com.yunseong.phonemonitor.live.LiveUpdateService
import com.yunseong.phonemonitor.widget.ClassicWidgetReceiver
import com.yunseong.phonemonitor.widget.FocusWidgetReceiver
import com.yunseong.phonemonitor.widget.GradientWidgetReceiver
import com.yunseong.phonemonitor.widget.RingWidgetReceiver
import com.yunseong.phonemonitor.widget.TerminalWidgetReceiver
import androidx.compose.ui.graphics.Brush
import java.text.DateFormat
import java.util.Date

@Composable
fun DashboardScreen(onOpenSettings: () -> Unit, vm: DashboardViewModel = viewModel()) {
    val stats by vm.stats.collectAsStateWithLifecycle()
    val history by vm.netHistory.collectAsStateWithLifecycle()
    val s = stats
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (s == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            return@Box
        }
        val insets = WindowInsets.safeDrawing.asPaddingValues()
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = insets.calculateTopPadding() + 12.dp,
                bottom = insets.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Header(s, onOpenSettings, Modifier.entrance(0)) }
            item { Gauges(s, Modifier.entrance(1)) }
            item { BatteryCard(s.battery, Modifier.entrance(2)) }
            item { CpuCard(s.cpu, Modifier.entrance(3)) }
            item { ThermalCard(s.thermal, Modifier.entrance(4)) }
            item { NetworkCard(s, history, Modifier.entrance(5)) }
            item { MemoryStorageCard(s, Modifier.entrance(6)) }
            item { LiveCard(Modifier.entrance(7)) }
            item { WidgetsCard(Modifier.entrance(8)) }
        }
        // 스크롤한 카드가 상태 표시줄 글자와 겹치지 않게 가린다
        Box(
            Modifier
                .fillMaxWidth()
                .height(insets.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
        )
    }
}

@Composable
private fun Header(s: DeviceStats, onOpenSettings: () -> Unit, modifier: Modifier) {
    Column(modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}",
                style = MaterialTheme.typography.labelMedium,
                color = LocalSurfaces.current.muted,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(LocalSurfaces.current.card).clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(R.drawable.ic_settings), stringResource(R.string.settings),
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
            PulseDot(Accent.battery[0])
            Text(
                DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(s.readAt)),
                style = MaterialTheme.typography.labelMedium,
                color = LocalSurfaces.current.muted,
            )
        }
    }
}

@Composable
private fun Gauges(s: DeviceStats, modifier: Modifier) {
    Card(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            RingGauge(s.battery.percent / 100f, Accent.battery, stringResource(R.string.card_battery), "${s.battery.percent}%", Modifier.weight(1f))
            RingGauge(s.memory.usedPercent / 100f, Accent.memory, stringResource(R.string.label_ram), "${s.memory.usedPercent}%", Modifier.weight(1f))
            RingGauge(s.storage.usedPercent / 100f, Accent.storage, stringResource(R.string.card_storage), "${s.storage.usedPercent}%", Modifier.weight(1f))
        }
    }
}

@Composable
private fun BatteryCard(b: BatteryStats, modifier: Modifier) {
    val charging = b.status == BatteryManager.BATTERY_STATUS_CHARGING
    Card(modifier) {
        CardTitle(stringResource(R.string.card_battery), Accent.battery) {
            if (charging) PulseDot(Accent.battery[0])
        }
        val na = stringResource(R.string.not_supported)
        TileGrid(
            listOf(
                stringResource(R.string.label_temperature) to (b.temperatureC?.let(::formatTemp) ?: na),
                stringResource(R.string.label_status) to stringResource(batteryStatusLabel(b.status)),
                stringResource(R.string.label_power) to stringResource(pluggedLabel(b.plugged)),
                stringResource(R.string.label_health) to stringResource(healthLabel(b.health)),
                stringResource(R.string.label_voltage) to (b.voltageMv?.let { "%.2f V".format(it / 1000f) } ?: na),
                stringResource(R.string.label_current) to (b.currentMa?.let { "$it mA" } ?: na),
            )
        )
    }
}

@Composable
private fun TileGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CpuCard(cpu: CpuStats, modifier: Modifier) {
    Card(modifier) {
        CardTitle(stringResource(R.string.card_cpu), Accent.cpu) {
            cpu.fastestKhz?.let { Text(formatKhz(it), style = MaterialTheme.typography.labelMedium, color = LocalSurfaces.current.muted) }
        }
        if (cpu.unavailable) {
            Text(stringResource(R.string.cpu_unavailable), color = LocalSurfaces.current.muted)
            return@Card
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cpu.cores.forEach { core ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.cpu_core, core.index),
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalSurfaces.current.muted,
                        modifier = Modifier.width(56.dp),
                    )
                    val cur = core.curKhz
                    val max = core.maxKhz
                    Bar(if (cur != null && max != null && max > 0) cur.toFloat() / max else 0f, Accent.cpu, Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    RollingText(
                        cur?.let(::formatKhz) ?: stringResource(R.string.cpu_core_offline),
                        fontSize = 13.sp,
                        modifier = Modifier.width(76.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.cpu_note), style = MaterialTheme.typography.labelMedium, color = LocalSurfaces.current.muted)
    }
}

@Composable
private fun ThermalCard(t: ThermalStats, modifier: Modifier) {
    val target = when {
        t.status >= PowerManager.THERMAL_STATUS_SEVERE -> Accent.danger
        t.status >= PowerManager.THERMAL_STATUS_LIGHT -> Accent.warn
        else -> Accent.battery[0]
    }
    val color by animateColorAsState(target, tween(600), label = "thermal")
    Card(modifier) {
        CardTitle(stringResource(R.string.card_thermal), listOf(color, color)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(stringResource(thermalLabel(t.status)), color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        val headroom = t.headroom
        if (headroom != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.label_headroom), style = MaterialTheme.typography.labelMedium, color = LocalSurfaces.current.muted, modifier = Modifier.weight(1f))
                RollingText("${(headroom * 100).toInt()}%", fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            Bar(headroom, listOf(Accent.battery[0], color))
            Spacer(Modifier.height(8.dp))
        }
        Text(stringResource(R.string.thermal_note), style = MaterialTheme.typography.labelMedium, color = LocalSurfaces.current.muted)
    }
}

@Composable
private fun NetworkCard(s: DeviceStats, history: List<Long>, modifier: Modifier) {
    val measuring = stringResource(R.string.measuring)
    Card(modifier) {
        CardTitle(stringResource(R.string.card_network), Accent.network)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("↓ " + stringResource(R.string.label_download), formatRate(s.network.rxBytesPerSec) ?: measuring, Modifier.weight(1f))
            StatTile("↑ " + stringResource(R.string.label_upload), formatRate(s.network.txBytesPerSec) ?: measuring, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Sparkline(history, Accent.network)
    }
}

@Composable
private fun MemoryStorageCard(s: DeviceStats, modifier: Modifier) {
    Card(modifier) {
        CardTitle(stringResource(R.string.card_memory), Accent.memory)
        UsageRow(s.memory.usedBytes, s.memory.availableBytes, s.memory.totalBytes, Accent.memory)
        Spacer(Modifier.height(20.dp))
        CardTitle(stringResource(R.string.card_storage), Accent.storage)
        UsageRow(s.storage.usedBytes, s.storage.availableBytes, s.storage.totalBytes, Accent.storage)
        Spacer(Modifier.height(20.dp))
        CleanupButton()
    }
}

/** 휴대폰의 정리 화면(삼성 디바이스 케어 등)으로 이동. 앱이 직접 정리하지는 않는다 */
@Composable
private fun CleanupButton() {
    val context = LocalContext.current
    val target = remember { Cleanup.best(context) }
    val failed = stringResource(R.string.cleanup_failed)
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Accent.memory[1], Color(0xFFEC4899), Accent.storage[1])))
            .clickable {
                if (!Cleanup.open(context)) Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.cleanup_button), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        listOfNotNull(
            target?.let { stringResource(R.string.cleanup_opens, stringResource(it.label)) },
            stringResource(R.string.cleanup_note),
        ).joinToString("\n"),
        style = MaterialTheme.typography.labelMedium,
        color = LocalSurfaces.current.muted,
    )
}

@Composable
private fun UsageRow(used: Long, available: Long, total: Long, colors: List<Color>) {
    Bar(if (total > 0) used.toFloat() / total else 0f, colors, height = 10.dp)
    Spacer(Modifier.height(10.dp))
    Row {
        LabeledValue(stringResource(R.string.label_used), formatBytes(used), Modifier.weight(1f))
        LabeledValue(stringResource(R.string.label_available), formatBytes(available), Modifier.weight(1f))
        LabeledValue(stringResource(R.string.label_total), formatBytes(total), Modifier.weight(1f))
    }
}

@Composable
private fun LabeledValue(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = LocalSurfaces.current.muted)
        RollingText(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LiveCard(modifier: Modifier) {
    val context = LocalContext.current
    val running by LiveUpdateService.running.collectAsStateWithLifecycle()
    var resumed by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumed++
        onPauseOrDispose { }
    }
    val notificationsOn = remember(resumed, running) { AppPermission.NOTIFICATIONS.isGranted(context) }
    // 거절해도 서비스는 돈다. 알림바에 보이지 않을 뿐
    val requestNotifications = rememberPermissionRequest(AppPermission.NOTIFICATIONS) { LiveUpdateService.enable(context) }
    Card(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                CardTitle(stringResource(R.string.card_live), Accent.memory)
            }
            Switch(
                checked = running,
                onCheckedChange = { on ->
                    if (!on) LiveUpdateService.disable(context)
                    else if (notificationsOn) LiveUpdateService.enable(context)
                    else requestNotifications()
                },
                colors = SwitchDefaults.colors(checkedTrackColor = Accent.memory[1]),
            )
        }
        Text(
            stringResource(if (running) R.string.live_description else R.string.live_off_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalSurfaces.current.muted,
        )
        if (running && !notificationsOn) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.live_no_notification),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Accent.warn,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = requestNotifications) { Text(stringResource(R.string.allow)) }
            }
        }
    }
}

private data class WidgetOption(val receiver: Class<*>, val name: Int, val description: Int, val colors: List<Color>)

private val widgetOptions = listOf(
    WidgetOption(ClassicWidgetReceiver::class.java, R.string.widget_classic, R.string.widget_classic_description, Accent.battery),
    WidgetOption(RingWidgetReceiver::class.java, R.string.widget_ring, R.string.widget_ring_description, Accent.memory),
    WidgetOption(GradientWidgetReceiver::class.java, R.string.widget_gradient, R.string.widget_gradient_description, listOf(Color(0xFF7C3AED), Color(0xFF0D9488))),
    WidgetOption(TerminalWidgetReceiver::class.java, R.string.widget_terminal, R.string.widget_terminal_description, listOf(Color(0xFF3FB950), Color(0xFF0D1117))),
    WidgetOption(FocusWidgetReceiver::class.java, R.string.widget_focus, R.string.widget_focus_description, Accent.storage),
)

/** 위젯 모양 5가지. 누르면 홈 화면에 바로 놓는 창이 뜬다 */
@Composable
private fun WidgetsCard(modifier: Modifier) {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return
    Card(modifier) {
        CardTitle(stringResource(R.string.card_widgets), Accent.cpu)
        Text(stringResource(R.string.widgets_hint), style = MaterialTheme.typography.bodyMedium, color = LocalSurfaces.current.muted)
        widgetOptions.forEach { option ->
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(option.colors)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(option.name), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(option.description), style = MaterialTheme.typography.labelMedium, color = LocalSurfaces.current.muted)
                }
                FilledTonalButton(onClick = {
                    manager.requestPinAppWidget(ComponentName(context, option.receiver), null, null)
                }) { Text(stringResource(R.string.add)) }
            }
        }
    }
}
