package com.yunseong.phonemonitor.live

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.yunseong.phonemonitor.MainActivity
import com.yunseong.phonemonitor.R
import com.yunseong.phonemonitor.data.DeviceStats
import com.yunseong.phonemonitor.data.StatsStore
import com.yunseong.phonemonitor.data.formatTemp
import com.yunseong.phonemonitor.permission.prefs
import com.yunseong.phonemonitor.widget.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 켜 두는 동안 위젯과 알림을 몇 초마다 새로 그린다 */
class LiveUpdateService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // 알림의 [끄기]는 사용자가 끈 것이므로 재부팅 뒤에도 꺼 둔다
            setEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }
        createChannel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(StatsStore.stats.value), type)
        } catch (e: Exception) {
            // 백그라운드에서 다시 시작될 때 일부 기기·버전이 막는다 (ForegroundServiceStartNotAllowedException 등)
            Log.w(TAG, "startForeground failed", e)
            stopSelf()
            return START_NOT_STICKY
        }
        _running.value = true
        if (loop == null) loop = scope.launch { run() }
        return START_STICKY
    }

    private suspend fun run() {
        val power = getSystemService(PowerManager::class.java)
        val notifications = getSystemService(NotificationManager::class.java)
        while (scope.isActive) {
            // 화면이 꺼져 있으면 아무도 안 보므로 건너뛴다 (배터리 절약)
            if (power.isInteractive) {
                // 한 번 실패해도 다음 회차에 다시 시도한다
                try {
                    val stats = StatsStore.refresh(this)
                    Widgets.updateAll(this)
                    if (stats != null) notifications.notify(NOTIFICATION_ID, notification(stats))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "update failed", e)
                }
            }
            delay(INTERVAL_MS)
        }
    }

    override fun onDestroy() {
        _running.value = false
        scope.cancel()
        // 실시간 표시를 지우려고 마지막으로 한 번 더 그린다
        CoroutineScope(Dispatchers.Default).launch { runCatching { Widgets.updateAll(applicationContext) } }
        super.onDestroy()
    }

    private fun notification(stats: DeviceStats?): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, LiveUpdateService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val text = stats?.let {
            getString(
                R.string.notification_text,
                "${it.battery.percent}%",
                it.battery.temperatureC?.let(::formatTemp) ?: "-",
                it.memory.usedPercent,
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.notification_stop), stop)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "LiveUpdateService"
        private const val CHANNEL_ID = "live"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "stop"
        private const val INTERVAL_MS = 3_000L
        private const val PREF_ENABLED = "live_enabled"

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        /** 사용자가 켠 상태. 재부팅·앱 업데이트·강제 종료 뒤 이 값대로 다시 켠다 */
        fun isEnabled(context: Context) = prefs(context).getBoolean(PREF_ENABLED, false)

        private fun setEnabled(context: Context, on: Boolean) = prefs(context).edit { putBoolean(PREF_ENABLED, on) }

        /** 스위치를 켰을 때 */
        fun enable(context: Context) {
            setEnabled(context, true)
            start(context)
        }

        /** 스위치를 껐을 때 */
        fun disable(context: Context) {
            setEnabled(context, false)
            stop(context)
        }

        /** 켜 둔 상태인데 돌고 있지 않으면 다시 켠다 (앱을 열 때, 부팅 뒤) */
        fun restoreIfEnabled(context: Context) {
            if (isEnabled(context) && !running.value) start(context)
        }

        /** 시작하지 못하면 false (앱이 백그라운드일 때 등). 스위치는 running으로 실제 상태를 보여 준다 */
        fun start(context: Context): Boolean = try {
            ContextCompat.startForegroundService(context, Intent(context, LiveUpdateService::class.java))
            true
        } catch (e: Exception) {
            Log.w(TAG, "start failed", e)
            false
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LiveUpdateService::class.java))
        }
    }
}
