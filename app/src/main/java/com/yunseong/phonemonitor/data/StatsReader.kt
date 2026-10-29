package com.yunseong.phonemonitor.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import java.io.File
import kotlin.math.abs

/**
 * 기기 상태를 읽는다. 네트워크 속도와 발열 여유는 직전 값이 필요해서 앱 전체에서 하나만 쓴다 (get).
 */
class StatsReader private constructor(context: Context) {
    private val app = context.applicationContext
    private val activityManager = app.getSystemService(ActivityManager::class.java)
    private val batteryManager = app.getSystemService(BatteryManager::class.java)
    private val powerManager = app.getSystemService(PowerManager::class.java)
    private val coreCount = readCoreCount()

    private var lastRx = -1L
    private var lastTx = -1L
    private var lastNetAt = 0L
    private var headroom: Float? = null
    private var headroomAt = 0L

    /** 항목 하나가 실패해도(제조사별 예외 등) 나머지는 보여 준다. 실패한 항목은 '알 수 없음' 값 */
    @Synchronized
    fun read(): DeviceStats = DeviceStats(
        battery = safe("battery", ::battery) { BatteryStats(0, null, null, null, BatteryManager.BATTERY_STATUS_UNKNOWN, 0, BatteryManager.BATTERY_HEALTH_UNKNOWN) },
        memory = safe("memory", ::memory) { MemoryStats(0, 0, false) },
        storage = safe("storage", ::storage) { StorageStats(0, 0) },
        cpu = safe("cpu", ::cpu) { CpuStats(emptyList()) },
        thermal = safe("thermal", ::thermal) { ThermalStats(PowerManager.THERMAL_STATUS_NONE, null) },
        network = safe("network", ::network) { NetworkStats(null, null) },
        readAt = System.currentTimeMillis(),
    )

    private inline fun <T> safe(name: String, block: () -> T, fallback: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "read $name failed", e)
            fallback()
        }

    private fun battery(): BatteryStats {
        // 등록하지 않고 마지막 배터리 방송만 받아 온다 (sticky)
        val intent = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (level >= 0 && scale > 0) level * 100 / scale
        else batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?.takeIf { it != Int.MIN_VALUE }?.div(10f)
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }
        return BatteryStats(
            percent = percent.coerceIn(0, 100),
            temperatureC = temp,
            voltageMv = voltage,
            currentMa = currentMa(),
            status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                ?: BatteryManager.BATTERY_STATUS_UNKNOWN,
            plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0,
            health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                ?: BatteryManager.BATTERY_HEALTH_UNKNOWN,
        )
    }

    private fun currentMa(): Int? {
        val raw = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        if (raw == Int.MIN_VALUE || raw == 0) return null
        // 규격은 µA인데 mA로 주는 기기가 있다. 휴대폰 전류가 10A를 넘을 일은 없으므로 크기로 구분
        return if (abs(raw) > 10_000) raw / 1000 else raw
    }

    private fun memory(): MemoryStats {
        val info = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        return MemoryStats(info.totalMem, info.availMem, info.lowMemory)
    }

    private fun storage(): StorageStats {
        val fs = StatFs(Environment.getDataDirectory().path)
        return StorageStats(fs.totalBytes, fs.availableBytes)
    }

    private fun cpu(): CpuStats = CpuStats(
        (0 until coreCount).map { i ->
            val dir = "/sys/devices/system/cpu/cpu$i/cpufreq"
            // 에뮬레이터의 가짜 드라이버는 1~2kHz를 준다. 10MHz 미만은 못 읽은 것으로 본다
            CoreFreq(
                i,
                readLong("$dir/scaling_cur_freq")?.takeIf { it >= 10_000 },
                readLong("$dir/cpuinfo_max_freq")?.takeIf { it >= 10_000 },
            )
        }
    )

    private fun thermal(): ThermalStats {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 너무 자주 부르면 NaN이 오므로 2초에 한 번만 새로 묻는다
            val now = SystemClock.elapsedRealtime()
            if (now - headroomAt >= 2_000) {
                headroomAt = now
                headroom = powerManager.getThermalHeadroom(0).takeIf { !it.isNaN() } ?: headroom
            }
        }
        return ThermalStats(powerManager.currentThermalStatus, headroom)
    }

    private fun network(): NetworkStats {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - lastNetAt
        val usable = lastRx >= 0 && rx >= lastRx && tx >= lastTx && elapsed in 1..10_000
        val result = if (usable) NetworkStats(
            rxBytesPerSec = (rx - lastRx) * 1000 / elapsed,
            txBytesPerSec = (tx - lastTx) * 1000 / elapsed,
        ) else NetworkStats(null, null)
        if (rx != TrafficStats.UNSUPPORTED.toLong()) {
            lastRx = rx
            lastTx = tx
            lastNetAt = now
        }
        return result
    }

    /** "0-7" 같은 형식. 못 읽으면 지금 켜진 코어 수 */
    private fun readCoreCount(): Int {
        val possible = runCatching { File("/sys/devices/system/cpu/possible").readText().trim() }.getOrNull()
        val last = possible?.substringAfterLast('-')?.toIntOrNull()
        return if (last != null) last + 1 else Runtime.getRuntime().availableProcessors()
    }

    private fun readLong(path: String): Long? =
        runCatching { File(path).readText().trim().toLong() }.getOrNull()

    companion object {
        private const val TAG = "StatsReader"
        @Volatile private var instance: StatsReader? = null

        fun get(context: Context): StatsReader =
            instance ?: synchronized(this) { instance ?: StatsReader(context).also { instance = it } }
    }
}
