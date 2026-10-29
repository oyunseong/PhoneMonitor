package com.yunseong.phonemonitor.data

/** 한 번 읽은 기기 상태. 기기가 알려 주지 않는 값은 null. */
data class DeviceStats(
    val battery: BatteryStats,
    val memory: MemoryStats,
    val storage: StorageStats,
    val cpu: CpuStats,
    val thermal: ThermalStats,
    val network: NetworkStats,
    val readAt: Long,
)

data class BatteryStats(
    val percent: Int,
    val temperatureC: Float?,
    val voltageMv: Int?,
    /** 양수면 충전, 음수면 방전. 기기마다 부호·단위가 다를 수 있다 */
    val currentMa: Int?,
    val status: Int,
    val plugged: Int,
    val health: Int,
)

data class MemoryStats(
    val totalBytes: Long,
    val availableBytes: Long,
    val low: Boolean,
) {
    val usedBytes get() = totalBytes - availableBytes
    val usedPercent get() = if (totalBytes > 0) (usedBytes * 100 / totalBytes).toInt() else 0
}

data class StorageStats(
    val totalBytes: Long,
    val availableBytes: Long,
) {
    val usedBytes get() = totalBytes - availableBytes
    val usedPercent get() = if (totalBytes > 0) (usedBytes * 100 / totalBytes).toInt() else 0
}

data class CpuStats(val cores: List<CoreFreq>) {
    /** 코어 속도를 하나도 읽지 못한 기기 */
    val unavailable get() = cores.all { it.curKhz == null }
    val fastestKhz get() = cores.mapNotNull { it.curKhz }.maxOrNull()
}

/** curKhz가 null이면 꺼진 코어이거나 읽기가 막힌 기기 */
data class CoreFreq(val index: Int, val curKhz: Long?, val maxKhz: Long?)

data class ThermalStats(
    /** PowerManager.THERMAL_STATUS_* */
    val status: Int,
    /** 1.0이면 성능 제한 시작. Android 11 미만이거나 기기가 지원하지 않으면 null */
    val headroom: Float?,
)

/** 첫 측정이거나 직전 측정과 10초 넘게 떨어져 있으면 속도는 null */
data class NetworkStats(
    val rxBytesPerSec: Long?,
    val txBytesPerSec: Long?,
)
