# Neptune retained WebView fork

Based on upstream `2.0.3`. Opt-in Android/iOS native view retention lets a
navigation back-stack entry own the renderer across composition removal.

```kotlin
// Own this state in the destination's ViewModel, not remember inside its UI.
val webViewState = WebViewState(WebContent.NavigatorOnly, retainNativeWebView = true)

// Render with WebView(state = webViewState, ...).
// In the ViewModel's onCleared():
webViewState.dispose()
```

Use a separate state for each back-stack entry. A retained state may only be
attached to one interop container at a time. Dispose on the UI thread when the
owner is removed. Disposal waits for an attached container to release its view.
Android host destruction evicts the renderer to avoid retaining an old Activity.
Process death and Activity recreation may therefore require a fresh load.

On temporary removal, platform callbacks and JS bridge handlers are detached;
on return they are rebound to the current composition. The JS bridge preserves
its in-page callback registry. Declarative content loads are deduplicated by
native instance identity and content. Explicit navigator commands still run,
but are queued rather than replayed when a collector restarts.

The default remains composition-scoped. Desktop and Wasm do not implement native
retention. Android/iOS browsing-state restoration alone does not preserve a live
DOM or JS heap, so this opt-in retains the native renderer in memory.

Build tooling matches Neptune (AGP 8.13.2, Kotlin 2.3.0, Compose 1.9.3) for Gradle
source dependency compatibility. Samples are opt-in with `-Pwebview.samples=true`.
Common lifecycle/load/navigation regression tests run via `:webview:desktopTest`.
