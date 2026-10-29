package com.yunseong.phonemonitor.data

import android.os.BatteryManager
import android.os.PowerManager
import androidx.annotation.StringRes
import com.yunseong.phonemonitor.R
import java.util.Locale

fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes B" else String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}

fun formatRate(bytesPerSec: Long?): String? = bytesPerSec?.let { "${formatBytes(it)}/s" }

fun formatKhz(khz: Long): String =
    if (khz >= 1_000_000) String.format(Locale.getDefault(), "%.2f GHz", khz / 1_000_000.0)
    else "${khz / 1000} MHz"

fun formatTemp(celsius: Float): String = String.format(Locale.getDefault(), "%.1f°C", celsius)

@StringRes
fun batteryStatusLabel(status: Int): Int = when (status) {
    BatteryManager.BATTERY_STATUS_CHARGING -> R.string.status_charging
    BatteryManager.BATTERY_STATUS_DISCHARGING -> R.string.status_discharging
    BatteryManager.BATTERY_STATUS_FULL -> R.string.status_full
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> R.string.status_not_charging
    else -> R.string.unknown
}

@StringRes
fun pluggedLabel(plugged: Int): Int = when (plugged) {
    BatteryManager.BATTERY_PLUGGED_AC -> R.string.power_ac
    BatteryManager.BATTERY_PLUGGED_USB -> R.string.power_usb
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> R.string.power_wireless
    BatteryManager.BATTERY_PLUGGED_DOCK -> R.string.power_dock
    else -> R.string.power_battery
}

@StringRes
fun healthLabel(health: Int): Int = when (health) {
    BatteryManager.BATTERY_HEALTH_GOOD -> R.string.health_good
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> R.string.health_overheat
    BatteryManager.BATTERY_HEALTH_DEAD -> R.string.health_dead
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> R.string.health_over_voltage
    BatteryManager.BATTERY_HEALTH_COLD -> R.string.health_cold
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> R.string.health_failure
    else -> R.string.unknown
}

@StringRes
fun thermalLabel(status: Int): Int = when (status) {
    PowerManager.THERMAL_STATUS_NONE -> R.string.thermal_none
    PowerManager.THERMAL_STATUS_LIGHT -> R.string.thermal_light
    PowerManager.THERMAL_STATUS_MODERATE -> R.string.thermal_moderate
    PowerManager.THERMAL_STATUS_SEVERE -> R.string.thermal_severe
    PowerManager.THERMAL_STATUS_CRITICAL -> R.string.thermal_critical
    PowerManager.THERMAL_STATUS_EMERGENCY -> R.string.thermal_emergency
    PowerManager.THERMAL_STATUS_SHUTDOWN -> R.string.thermal_shutdown
    else -> R.string.unknown
}
