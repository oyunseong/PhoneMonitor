package com.yunseong.phonemonitor.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yunseong.phonemonitor.R
import com.yunseong.phonemonitor.permission.AppPermission
import com.yunseong.phonemonitor.permission.rememberPermissionRequest

/** 스플래시 다음에 아직 없는 권한을 하나씩 묻는다. 거절해도 다음으로 넘어간다 */
@Composable
fun PermissionScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val steps = remember { AppPermission.missing(context) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    if (steps.isEmpty()) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val next: () -> Unit = { if (index + 1 < steps.size) index += 1 else onDone() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        Column(Modifier.align(Alignment.TopStart).padding(top = 24.dp)) {
            Text(stringResource(R.string.perm_intro_title), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.perm_intro_body), color = LocalSurfaces.current.muted)
        }
        AnimatedContent(
            targetState = index,
            modifier = Modifier.align(Alignment.Center),
            transitionSpec = {
                (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320)))
                    .togetherWith(slideOutHorizontally(tween(240)) { -it / 3 } + fadeOut(tween(240)))
            },
            label = "step",
        ) { i ->
            val permission = steps[i]
            val request = rememberPermissionRequest(permission) { next() }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(Brush.linearGradient(Accent.memory)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painterResource(permission.icon), null,
                        modifier = Modifier.size(44.dp),
                        colorFilter = ColorFilter.tint(Color.White),
                    )
                }
                if (steps.size > 1) {
                    Text(stringResource(R.string.perm_step, i + 1, steps.size), style = MaterialTheme.typography.labelMedium, color = LocalSurfaces.current.muted)
                }
                Text(stringResource(permission.title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(permission.reason), textAlign = TextAlign.Center, color = LocalSurfaces.current.muted)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = request,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent.memory[1]),
                ) { Text(stringResource(R.string.allow)) }
                TextButton(onClick = next) { Text(stringResource(R.string.later), color = LocalSurfaces.current.muted) }
            }
        }
    }
}
