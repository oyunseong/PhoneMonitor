package com.yunseong.phonemonitor.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val gaugeSpring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)

/**
 * 처음 나타날 때 순서대로 아래에서 올라온다.
 * LazyColumn에서 스크롤로 사라졌다 돌아오면 remember가 초기화되므로,
 * 이미 보여줬다는 사실은 rememberSaveable로 기억해 다시 재생하지 않는다.
 */
@Composable
fun Modifier.entrance(index: Int): Modifier {
    var shown by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (shown) return@LaunchedEffect
        shown = true
        progress.animateTo(1f, tween(durationMillis = 520, delayMillis = 70 * index))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 48.dp.toPx()
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(LocalSurfaces.current.card)
            .padding(20.dp),
        content = content,
    )
}

@Composable
fun CardTitle(text: String, colors: List<Color>, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Brush.linearGradient(colors)))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing()
    }
    Spacer(Modifier.height(16.dp))
}

/** 값이 바뀌면 위·아래로 굴러가며 바뀐다 */
@Composable
fun RollingText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            (slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(260)))
                .togetherWith(slideOutVertically(tween(200)) { -it / 2 } + fadeOut(tween(200)))
        },
        label = "rolling",
    ) { value ->
        Text(value, fontSize = fontSize, fontWeight = fontWeight, color = color, maxLines = 1)
    }
}

/** 270도 링 게이지 */
@Composable
fun RingGauge(
    fraction: Float,
    colors: List<Color>,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    size: Dp = 104.dp,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), gaugeSpring, label = "gauge")
    val track = LocalSurfaces.current.track
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // 폭이 모자라면 정사각형을 유지한 채 줄어든다. 정사각형이 아니면 rotate 때문에 호가 어긋난다
        Box(Modifier.widthIn(max = size).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = 10.dp.toPx()
                val side = this.size.minDimension - stroke
                val topLeft = Offset((this.size.width - side) / 2, (this.size.height - side) / 2)
                val arcSize = Size(side, side)
                drawArc(track, 135f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                rotate(90f) {
                    drawArc(
                        Brush.sweepGradient(listOf(colors[0], colors[1], colors[0])),
                        45f, 270f * animated, false, topLeft, arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            RollingText(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = LocalSurfaces.current.muted)
    }
}

/** 둥근 가로 막대. 값이 바뀌면 부드럽게 늘어난다 */
@Composable
fun Bar(fraction: Float, colors: List<Color>, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), gaugeSpring, label = "bar")
    val track = LocalSurfaces.current.track
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                val r = CornerRadius(this.size.height / 2)
                drawRoundRect(track, cornerRadius = r)
                if (animated > 0f) {
                    drawRoundRect(
                        Brush.horizontalGradient(colors),
                        size = Size(this.size.width * animated.coerceAtLeast(0.02f), this.size.height),
                        cornerRadius = r,
                    )
                }
            }
    )
}

/** 작은 라벨 + 값 */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(LocalSurfaces.current.track.copy(alpha = 0.45f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = LocalSurfaces.current.muted)
        RollingText(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 깜빡이는 점 (실시간 표시) */
@Composable
fun PulseDot(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        1f, 2.2f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "scale",
    )
    Box(modifier.size(18.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(8.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale; alpha = (2.2f - scale) / 1.2f }
                .clip(CircleShape)
                .background(color)
        )
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
    }
}

/** 최근 값 꺾은선. 새 값이 들어오면 모양이 부드럽게 바뀐다 */
@Composable
fun Sparkline(values: List<Long>, colors: List<Color>, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
    val peak by animateFloatAsState(max, tween(400), label = "peak")
    Canvas(modifier.fillMaxWidth().height(64.dp)) {
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v -> Offset(i * step, size.height - (v / peak).coerceAtMost(1f) * size.height * 0.9f) }
        val line = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val cur = points[i]
                val midX = (prev.x + cur.x) / 2
                cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(colors[0].copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, Brush.horizontalGradient(colors), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(colors[1], 4.dp.toPx(), points.last())
    }
}
