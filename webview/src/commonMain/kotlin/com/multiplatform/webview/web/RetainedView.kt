package com.multiplatform.webview.web

/** Owns a single native view between mounts. All calls must run on the UI thread. */
internal class RetainedView<T : Any> {
    var value: T? = null
        private set
    private var attached = false
    private var disposed = false
    private var release: ((T) -> Unit)? = null

    fun attach(create: () -> T, release: (T) -> Unit): T {
        check(!disposed) { "This WebViewState has been disposed" }
        check(!attached) { "A retained WebViewState cannot be attached to two hosts at once" }
        val view = value ?: create().also {
            value = it
            this.release = release
        }
        attached = true
        return view
    }

    fun detach(view: T) {
        if (value !== view) return
        attached = false
        if (disposed) evict(view)
    }

    /** Used when the native host is destroyed, including Android configuration changes. */
    fun evict(view: T) {
        if (value !== view) return
        val disposeView = release
        value = null
        release = null
        attached = false
        disposeView?.invoke(view)
    }

    /** Defer destruction until the interop container releases its attached view. */
    fun dispose() {
        disposed = true
        if (!attached) value?.let(::evict)
    }
}

/** Deduplicate declarative loads across effect restarts, without suppressing a new native view. */
internal class WebViewContentLoadState<T : Any> {
    private var view: T? = null
    private var content: WebContent? = null

    suspend fun load(view: T, content: WebContent, load: suspend () -> Unit) {
        if (content is WebContent.NavigatorOnly || (this.view === view && this.content == content)) return
        load()
        this.view = view
        this.content = content
    }

    fun forget(view: T) {
        if (this.view !== view) return
        this.view = null
        content = null
    }
}
