package com.yunseong.phonemonitor.cleanup

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.StringRes
import com.yunseong.phonemonitor.R

/**
 * 휴대폰에 있는 정리 화면으로 보낸다. Android 14부터 앱은 다른 앱을 종료할 수 없어서 직접 정리하지 않는다.
 * 제조사 화면은 버전마다 위치가 달라 후보를 차례로 시도하고, 마지막은 모든 기기에 있는 저장 공간 화면.
 * 다른 앱 패키지를 찾으려면 AndroidManifest의 <queries>에도 적어야 한다.
 */
object Cleanup {
    private const val TAG = "Cleanup"

    class Target(@param:StringRes val label: Int, private val build: (Context) -> Intent?) {
        fun intent(context: Context) = build(context)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun component(pkg: String, cls: String) = { _: Context -> Intent().setComponent(ComponentName(pkg, cls)) }
    private fun launch(pkg: String) = { c: Context -> c.packageManager.getLaunchIntentForPackage(pkg) }
    private fun action(action: String) = { _: Context -> Intent(action) }

    private val samsung = listOf(
        Target(R.string.cleanup_samsung_ram, component("com.samsung.android.lool", "com.samsung.android.sm.ui.ram.RamActivity")),
        Target(R.string.cleanup_samsung, component("com.samsung.android.lool", "com.samsung.android.sm.ui.dashboard.SmartManagerDashBoardActivity")),
    )
    private val xiaomi = listOf(
        Target(R.string.cleanup_xiaomi, action("miui.intent.action.GARBAGE_CLEANUP")),
        Target(R.string.cleanup_xiaomi, component("com.miui.cleanmaster", "com.miui.optimizecenter.MainActivity")),
        Target(R.string.cleanup_xiaomi_security, launch("com.miui.securitycenter")),
    )
    private val huawei = listOf(
        Target(R.string.cleanup_phone_manager, component("com.huawei.systemmanager", "com.huawei.systemmanager.spacecleanner.SpaceCleanActivity")),
        Target(R.string.cleanup_phone_manager, launch("com.huawei.systemmanager")),
        Target(R.string.cleanup_phone_manager, launch("com.hihonor.systemmanager")),
    )
    private val oppo = listOf(
        Target(R.string.cleanup_phone_manager, launch("com.oplus.phonemanager")),
        Target(R.string.cleanup_phone_manager, launch("com.coloros.phonemanager")),
    )
    private val vivo = listOf(
        Target(R.string.cleanup_vivo, launch("com.iqoo.secure")),
    )
    private val generic = listOf(
        Target(R.string.cleanup_storage, action(StorageManager.ACTION_MANAGE_STORAGE)),
        Target(R.string.cleanup_storage_settings, action(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)),
        Target(R.string.cleanup_storage_settings, action(Settings.ACTION_SETTINGS)),
    )

    /** 이 기기에서 시도할 순서. 제조사 화면 → 공통 화면 */
    fun targets(manufacturer: String = Build.MANUFACTURER): List<Target> {
        val m = manufacturer.lowercase()
        val vendor = when {
            "samsung" in m -> samsung
            listOf("xiaomi", "redmi", "poco").any { it in m } -> xiaomi
            listOf("huawei", "honor").any { it in m } -> huawei
            listOf("oppo", "realme", "oneplus").any { it in m } -> oppo
            listOf("vivo", "iqoo").any { it in m } -> vivo
            else -> emptyList()
        }
        return vendor + generic
    }

    /** 버튼 아래에 보여 줄 이동할 화면 이름. 열 수 있는 첫 후보 */
    fun best(context: Context): Target? = targets().firstOrNull { target ->
        target.intent(context)?.resolveActivity(context.packageManager) != null
    }

    /** 열리는 화면이 나올 때까지 차례로 시도한다. 하나도 못 열면 false */
    fun open(context: Context, manufacturer: String = Build.MANUFACTURER): Boolean {
        for (target in targets(manufacturer)) {
            val intent = target.intent(context) ?: continue
            try {
                context.startActivity(intent)
                return true
            } catch (e: ActivityNotFoundException) {
                Log.i(TAG, "not found: $intent")
            } catch (e: SecurityException) {
                // 다른 앱이 외부에 열어 두지 않은 화면
                Log.i(TAG, "not exported: $intent")
            }
        }
        return false
    }
}
