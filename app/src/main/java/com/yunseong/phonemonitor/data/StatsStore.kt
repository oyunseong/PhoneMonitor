package com.yunseong.phonemonitor.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 마지막으로 읽은 상태. 앱 화면·위젯·알림이 같은 값을 본다.
 * 위젯은 이미 떠 있으면 provideGlance를 다시 부르지 않으므로, 새 값은 여기를 거쳐 전달한다.
 */
object StatsStore {
    private val _stats = MutableStateFlow<DeviceStats?>(null)
    val stats: StateFlow<DeviceStats?> = _stats.asStateFlow()

    /** 실패하면 마지막 값을 그대로 두고 null을 돌려준다 (화면·위젯이 죽지 않게) */
    suspend fun refresh(context: Context): DeviceStats? = withContext(Dispatchers.IO) {
        try {
            StatsReader.get(context).read().also { _stats.value = it }
        } catch (e: Exception) {
            Log.w("StatsStore", "refresh failed", e)
            null
        }
    }
}
