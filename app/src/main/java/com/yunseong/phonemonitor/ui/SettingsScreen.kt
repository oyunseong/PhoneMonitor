package com.yunseong.phonemonitor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.yunseong.phonemonitor.R
import com.yunseong.phonemonitor.permission.AppPermission
import com.yunseong.phonemonitor.permission.appSettingsIntent
import com.yunseong.phonemonitor.permission.batteryOptimizationIntent
import com.yunseong.phonemonitor.permission.isIgnoringBatteryOptimizations
import com.yunseong.phonemonitor.permission.openSettings
import com.yunseong.phonemonitor.permission.rememberPermissionRequest

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // 시스템 설정에서 돌아오면 상태를 다시 읽는다
    var resumed by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumed++
        onPauseOrDispose { }
    }
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = insets.calculateTopPadding() + 8.dp,
            bottom = insets.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painterResource(R.drawable.ic_back), stringResource(R.string.back),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.displaySmall)
            }
        }
        item {
            Card(Modifier.entrance(0)) {
                CardTitle(stringResource(R.string.settings_permissions), Accent.memory)
                AppPermission.entries.forEachIndexed { i, permission ->
                    if (i > 0) Spacer(Modifier.height(16.dp))
                    PermissionRow(permission, resumed)
                }
            }
        }
        item {
            Card(Modifier.entrance(1)) {
                CardTitle(stringResource(R.string.settings_optional), Accent.battery)
                val ignoring = remember(resumed) { isIgnoringBatteryOptimizations(context) }
                SettingRow(
                    icon = R.drawable.ic_battery_opt,
                    title = stringResource(R.string.battery_opt),
                    reason = stringResource(R.string.battery_opt_reason),
                    ok = ignoring,
                    okLabel = stringResource(R.string.battery_opt_on),
                    noLabel = stringResource(R.string.battery_opt_off),
                    action = stringResource(R.string.open_settings),
                    onAction = { context.openSettings(batteryOptimizationIntent()) },
                )
            }
        }
        item {
            Card(Modifier.entrance(2)) {
                CardTitle(stringResource(R.string.settings_about), Accent.cpu)
                val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
                Row {
                    Text(stringResource(R.string.version), color = LocalSurfaces.current.muted, modifier = Modifier.weight(1f))
                    Text(version ?: "-", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(
                    onClick = { context.openSettings(appSettingsIntent(context)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.app_system_settings)) }
            }
        }
    }
}

@Composable
private fun PermissionRow(permission: AppPermission, resumed: Int) {
    val context = LocalContext.current
    var granted by remember(resumed) { mutableStateOf(permission.isGranted(context)) }
    val request = rememberPermissionRequest(permission) { granted = it }
    SettingRow(
        icon = permission.icon,
        title = stringResource(permission.title),
        reason = stringResource(permission.reason),
        ok = granted,
        okLabel = stringResource(R.string.granted),
        noLabel = stringResource(R.string.denied),
        action = stringResource(R.string.allow),
        onAction = request,
    )
}

@Composable
private fun SettingRow(
    icon: Int,
    title: String,
    reason: String,
    ok: Boolean,
    okLabel: String,
    noLabel: String,
    action: String,
    onAction: () -> Unit,
) {
    val color = if (ok) Accent.battery[0] else Accent.warn
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(icon), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(color))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(if (ok) okLabel else noLabel, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(reason, style = MaterialTheme.typography.bodyMedium, color = LocalSurfaces.current.muted)
            if (!ok) {
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = onAction) { Text(action) }
            }
        }
    }
}
