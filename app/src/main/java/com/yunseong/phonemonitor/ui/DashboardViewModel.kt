package com.yunseong.phonemonitor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yunseong.phonemonitor.data.DeviceStats
import com.yunseong.phonemonitor.data.StatsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

private const val HISTORY = 40

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val _netHistory = MutableStateFlow<List<Long>>(emptyList())
    /** 최근 다운로드+업로드 속도 (스파크라인용) */
    val netHistory: StateFlow<List<Long>> = _netHistory.asStateFlow()

    /** 화면이 보일 때만 1초마다 읽는다 */
    val stats: StateFlow<DeviceStats?> = flow {
        while (true) {
            val s = StatsStore.refresh(getApplication())
            if (s != null) {
                val rx = s.network.rxBytesPerSec
                val tx = s.network.txBytesPerSec
                if (rx != null && tx != null) _netHistory.value = (_netHistory.value + (rx + tx)).takeLast(HISTORY)
                emit(s)
            }
            delay(1_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsStore.stats.value)
}
