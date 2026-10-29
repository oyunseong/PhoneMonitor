package com.yunseong.phonemonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yunseong.phonemonitor.live.LiveUpdateService
import com.yunseong.phonemonitor.permission.prefs
import com.yunseong.phonemonitor.ui.DashboardScreen
import com.yunseong.phonemonitor.ui.PermissionScreen
import com.yunseong.phonemonitor.ui.PhoneMonitorTheme
import com.yunseong.phonemonitor.ui.SettingsScreen

private enum class Screen { Permissions, Dashboard, Settings }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 강제 종료 등으로 꺼졌으면 앱을 열 때 다시 켠다
        LiveUpdateService.restoreIfEnabled(this)
        // 권한 안내는 처음 한 번만. 이후에는 필요한 순간과 설정 화면에서 묻는다
        val onboarded = prefs(this).getBoolean("onboarded", false)
        setContent {
            PhoneMonitorTheme {
                var screen by rememberSaveable { mutableStateOf(if (onboarded) Screen.Dashboard else Screen.Permissions) }
                BackHandler(screen == Screen.Settings) { screen = Screen.Dashboard }
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally(tween(300)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally(tween(240)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(200)))
                    },
                    label = "screen",
                ) { current ->
                    when (current) {
                        Screen.Permissions -> PermissionScreen(onDone = {
                            prefs(this@MainActivity).edit { putBoolean("onboarded", true) }
                            screen = Screen.Dashboard
                        })
                        Screen.Dashboard -> DashboardScreen(onOpenSettings = { screen = Screen.Settings })
                        Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Dashboard })
                    }
                }
            }
        }
    }
}
