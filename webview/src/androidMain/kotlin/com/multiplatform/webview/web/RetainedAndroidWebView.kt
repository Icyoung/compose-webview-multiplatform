package com.multiplatform.webview.web

import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

internal fun WebViewState.attachRetainedAndroidView(context: Context, factory: () -> WebView): WebView {
    var hostLifecycle: Lifecycle? = null
    var hostObserver: LifecycleEventObserver? = null
    return retainedView.attach(
        create = {
            factory().also { native ->
                hostLifecycle = context.hostLifecycle()
                hostObserver = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_DESTROY) retainedView.evict(native)
                }
                hostObserver?.let { hostLifecycle?.addObserver(it) }
            }
        },
        release = { native ->
            hostObserver?.let { hostLifecycle?.removeObserver(it) }
            forgetNativeView(native)
            (native.parent as? ViewGroup)?.removeView(native)
            native.stopLoading()
            native.removeJavascriptInterface("androidJsBridge")
            native.destroy()
        },
    )
}

private fun Context.hostLifecycle(): Lifecycle? {
    var current: Context = this
    while (true) {
        if (current is LifecycleOwner) return current.lifecycle
        val base = (current as? ContextWrapper)?.baseContext ?: return null
        if (base === current) return null
        current = base
    }
}
