package com.yunseong.phonemonitor

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.compose
import com.yunseong.phonemonitor.widget.ClassicStatusWidget
import com.yunseong.phonemonitor.widget.FocusStatusWidget
import com.yunseong.phonemonitor.widget.GradientStatusWidget
import com.yunseong.phonemonitor.widget.RingStatusWidget
import com.yunseong.phonemonitor.widget.TerminalStatusWidget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** adb shell am start -n com.yunseong.phonemonitor/.WidgetGalleryActivity --es style ring */
class WidgetGalleryActivity : Activity() {
    private val scope = MainScope()

    @OptIn(ExperimentalGlanceApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgets = mapOf(
            "classic" to ClassicStatusWidget(), "ring" to RingStatusWidget(), "gradient" to GradientStatusWidget(),
            "terminal" to TerminalStatusWidget(), "focus" to FocusStatusWidget(),
        )
        // 다른 제조사 정리 화면 시도 시험: --es cleanup samsung
        intent.getStringExtra("cleanup")?.let {
            android.util.Log.i("Gallery", "cleanup $it opened=${com.yunseong.phonemonitor.cleanup.Cleanup.open(this, it)}")
            finish()
            return
        }
        val only = intent.getStringExtra("style")
        if (intent.getBooleanExtra("export", false)) {
            scope.launch { exportPreviews(widgets) }
            return
        }
        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 140, 24, 60) }
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.rgb(120, 140, 190)); addView(root) })

        // 1×1, 2×1, 2×2(낮은 기기) / 4×1 / 4×2(낮은 기기) / 2×2(높은 기기) / 4×2(높은 기기)
        val rows = listOf(
            listOf(DpSize(70.dp, 70.dp), DpSize(140.dp, 70.dp), DpSize(150.dp, 140.dp)),
            listOf(DpSize(320.dp, 70.dp)),
            listOf(DpSize(320.dp, 140.dp)),
            listOf(DpSize(150.dp, 190.dp)),
            listOf(DpSize(320.dp, 190.dp)),
        )
        scope.launch {
            for ((name, widget) in widgets.filterKeys { only == null || it == only }) {
                root.addView(TextView(this@WidgetGalleryActivity).apply { text = name; setTextColor(Color.WHITE); textSize = 16f })
                for (row in rows) {
                    val line = LinearLayout(this@WidgetGalleryActivity).apply { orientation = LinearLayout.HORIZONTAL }
                    for (size in row) {
                        val frame = FrameLayout(this@WidgetGalleryActivity)
                        android.util.Log.i("Gallery", "$name $size start")
                        val views = runCatching { widget.compose(this@WidgetGalleryActivity, size = size) }
                            .onFailure { android.util.Log.e("Gallery", "$name $size", it) }.getOrNull() ?: continue
                        android.util.Log.i("Gallery", "$name $size done")
                        frame.addView(views.apply(this@WidgetGalleryActivity, frame))
                        line.addView(frame, LinearLayout.LayoutParams((size.width.value * density).toInt(), (size.height.value * density).toInt()).apply { setMargins(0, 16, 24, 16) })
                    }
                    root.addView(line, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }
            }
        }
    }

    /** 위젯 고르기 화면용 미리보기 PNG. adb pull 후 res/drawable-nodpi/preview_<style>.png 로 넣는다 */
    @OptIn(ExperimentalGlanceApi::class)
    private suspend fun exportPreviews(widgets: Map<String, androidx.glance.appwidget.GlanceAppWidget>) {
        val density = resources.displayMetrics.density
        val dir = java.io.File(getExternalFilesDir(null), "previews").apply { mkdirs() }
        for ((name, widget) in widgets) {
            val size = if (name == "gradient" || name == "terminal") DpSize(320.dp, 190.dp) else DpSize(170.dp, 190.dp)
            val w = (size.width.value * density).toInt()
            val h = (size.height.value * density).toInt()
            val frame = FrameLayout(this)
            frame.addView(widget.compose(this, size = size).apply(this, frame))
            frame.measure(android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY))
            frame.layout(0, 0, w, h)
            val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            frame.draw(android.graphics.Canvas(bitmap))
            java.io.File(dir, "preview_$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            android.util.Log.i("Gallery", "exported $name")
        }
        // Play 스토어 아이콘 512×512 (모서리는 스토어가 깎는다)
        val icon = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(icon)
        for (layer in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
            getDrawable(layer)!!.apply { setBounds(0, 0, 512, 512) }.draw(canvas)
        }
        java.io.File(dir, "ic_launcher_512.png").outputStream().use { icon.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        android.util.Log.i("Gallery", "exported icon")
        finish()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
