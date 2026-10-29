package com.yunseong.phonemonitor.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 재부팅·앱 업데이트 뒤 실시간 모드를 켜 둔 상태였으면 다시 켠다 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> LiveUpdateService.restoreIfEnabled(context)
        }
    }
}
