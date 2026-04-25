package app.marlboroadvance.mpvex.ui.browser.shorts

import android.util.Xml
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.player.MPVView
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import org.xmlpull.v1.XmlPullParser

@Composable
fun ShortsPlayerHost(
    modifier: Modifier = Modifier,
    onReady: (MPVView) -> Unit,
    onPlayerReadyChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    
    val mpvView = remember {
        val parser = context.resources.getLayout(R.layout.shorts_dummy_layout)
        var type: Int
        while (parser.next().also { type = it } != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
        }
        val attrs = Xml.asAttributeSet(parser)
        
        MPVView(context, attrs).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    DisposableEffect(Unit) {
        val observer = object : MPVLib.EventObserver {
            override fun event(eventId: Int, data: MPVNode) {
                when (eventId) {
                    MPVLib.MpvEvent.MPV_EVENT_START_FILE -> {
                        onPlayerReadyChange(false)
                    }
                    MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
                        onPlayerReadyChange(true)
                    }
                }
            }
            override fun eventProperty(property: String) {}
            override fun eventProperty(property: String, value: Long) {}
            override fun eventProperty(property: String, value: Boolean) {}
            override fun eventProperty(property: String, value: String) {}
            override fun eventProperty(property: String, value: Double) {}
            override fun eventProperty(property: String, value: MPVNode) {}
        }

        mpvView.initialize(context.filesDir.path, context.cacheDir.path)
        MPVLib.addObserver(observer)
        onReady(mpvView)
        
        onDispose {
            MPVLib.removeObserver(observer)
            mpvView.destroy()
        }
    }

    AndroidView(
        factory = {
            FrameLayout(context).apply {
                addView(mpvView)
            }
        },
        modifier = modifier
    )
}
