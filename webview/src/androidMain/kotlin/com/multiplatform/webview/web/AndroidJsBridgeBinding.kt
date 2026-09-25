package com.multiplatform.webview.web

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.multiplatform.webview.R
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.WebViewJsBridge
import kotlinx.serialization.json.Json

/** The injected object must remain stable: Android only replaces it on the next page load. */
internal class AndroidJsBridgeBinding(private val nativeView: WebView) {
    @Volatile
    var target: WebViewJsBridge? = null

    @JavascriptInterface
    fun call(request: String) = dispatch(Json.decodeFromString<JsMessage>(request))

    @JavascriptInterface
    fun callAndroid(id: Int, method: String, params: String) = dispatch(JsMessage(id, method, params))

    private fun dispatch(message: JsMessage) {
        val recipient = target ?: return
        // Interface calls arrive on WebView's background thread. Bind/unbind and handlers
        // belong to the UI thread; discard messages from a mount that has since ended.
        nativeView.post {
            if (target === recipient) recipient.dispatch(message)
        }
    }
}

internal fun WebView.bindJsBridge(bridge: WebViewJsBridge) {
    val binding = getTag(R.id.retained_webview_js_bridge) as? AndroidJsBridgeBinding
        ?: AndroidJsBridgeBinding(this).also {
            setTag(R.id.retained_webview_js_bridge, it)
            addJavascriptInterface(it, "androidJsBridge")
        }
    binding.target = bridge
}

internal fun WebView.unbindJsBridge(bridge: WebViewJsBridge?) {
    val binding = getTag(R.id.retained_webview_js_bridge) as? AndroidJsBridgeBinding ?: return
    if (binding.target === bridge) binding.target = null
}

internal fun WebView.disposeJsBridge() {
    (getTag(R.id.retained_webview_js_bridge) as? AndroidJsBridgeBinding)?.target = null
    removeJavascriptInterface("androidJsBridge")
    setTag(R.id.retained_webview_js_bridge, null)
}
