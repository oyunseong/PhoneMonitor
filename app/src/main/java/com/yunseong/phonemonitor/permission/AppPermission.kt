package com.yunseong.phonemonitor.permission

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.yunseong.phonemonitor.R

/** 앱이 요청하는 런타임 권한. 시작 화면과 설정 화면이 이 목록을 그대로 쓴다 */
enum class AppPermission(
    val manifest: String,
    /** 이 버전부터 권한이 필요하다. 그 아래는 항상 허용으로 본다 */
    val sinceSdk: Int,
    @param:StringRes val title: Int,
    @param:StringRes val reason: Int,
    @param:DrawableRes val icon: Int,
) {
    NOTIFICATIONS(
        Manifest.permission.POST_NOTIFICATIONS,
        Build.VERSION_CODES.TIRAMISU,
        R.string.perm_notifications,
        R.string.perm_notifications_reason,
        R.drawable.ic_notification,
    );

    fun isRequired() = Build.VERSION.SDK_INT >= sinceSdk

    fun isGranted(context: Context) = !isRequired() ||
        ContextCompat.checkSelfPermission(context, manifest) == PackageManager.PERMISSION_GRANTED

    /** 두 번 거절하면 시스템이 더는 창을 띄우지 않는다. 그때는 설정 화면으로 보낸다 */
    fun isBlocked(activity: Activity) = asked(activity) && !isGranted(activity) &&
        !activity.shouldShowRequestPermissionRationale(manifest)

    fun settingsIntent(context: Context): Intent = when (this) {
        NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }

    private fun asked(context: Context) = prefs(context).getBoolean("asked_$name", false)

    fun markAsked(context: Context) = prefs(context).edit { putBoolean("asked_$name", true) }

    companion object {
        fun missing(context: Context) = entries.filter { it.isRequired() && !it.isGranted(context) }
    }
}

/** 제조사에 따라 없는 설정 화면이 있다. 없으면 앱 정보 화면, 그것도 없으면 아무 일도 하지 않는다 */
fun Context.openSettings(intent: Intent) {
    val tries = listOf(intent, appSettingsIntent(this))
    for (candidate in tries) {
        try {
            startActivity(candidate)
            return
        } catch (e: ActivityNotFoundException) {
            Log.w("Settings", "no activity for $candidate", e)
        }
    }
}

fun prefs(context: Context) = context.getSharedPreferences("app", Context.MODE_PRIVATE)

fun isIgnoringBatteryOptimizations(context: Context) =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

/** 배터리 최적화 목록 화면. 권한 없이 열 수 있는 화면으로 보낸다 (스토어 정책상 직접 요청 권한은 쓰지 않음) */
fun batteryOptimizationIntent() = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

fun appSettingsIntent(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

/**
 * 권한이 필요한 순간에 부른다. 이미 있으면 바로 onResult(true),
 * 물어볼 수 있으면 시스템 창, 막혀 있으면 설정 화면을 연다 (돌아왔을 때 화면이 다시 확인한다).
 */
@Composable
fun rememberPermissionRequest(permission: AppPermission, onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission.markAsked(context)
        onResult(granted)
    }
    return remember(permission, activity) {
        {
            when {
                permission.isGranted(context) -> onResult(true)
                activity != null && permission.isBlocked(activity) -> context.openSettings(permission.settingsIntent(context))
                else -> launcher.launch(permission.manifest)
            }
        }
    }
}
