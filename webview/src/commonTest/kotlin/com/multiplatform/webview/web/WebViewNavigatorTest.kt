package com.multiplatform.webview.web

import com.multiplatform.webview.jsbridge.WebViewJsBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WebViewNavigatorTest {
    @BeforeTest
    fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun restartingCollectorDoesNotReplayLoadOrBack() = runTest {
        val navigator = WebViewNavigator(backgroundScope)
        val view = RecordingWebView(backgroundScope)
        val first = backgroundScope.launch { with(navigator) { view.handleNavigationEvents() } }
        navigator.loadUrl("https://example.test")
        navigator.navigateBack()
        runCurrent()
        first.cancel()
        runCurrent()
        backgroundScope.launch { with(navigator) { view.handleNavigationEvents() } }
        runCurrent()
        assertEquals(listOf("https://example.test", "back"), view.events)
    }

    @Test
    fun commandsBeforeNativeViewIsReadyAreDeliveredOnceInOrder() = runTest {
        val navigator = WebViewNavigator(backgroundScope)
        val view = RecordingWebView(backgroundScope)
        navigator.loadUrl("https://example.test/first")
        navigator.loadUrl("https://example.test/second")
        runCurrent()
        backgroundScope.launch { with(navigator) { view.handleNavigationEvents() } }
        runCurrent()
        assertEquals(listOf("https://example.test/first", "https://example.test/second"), view.events)
    }
}

private class RecordingWebView(override val scope: CoroutineScope) : IWebView {
    val events = mutableListOf<String>()
    override val webView: NativeWebView get() = error("No native renderer needed for navigator tests")
    override val webViewJsBridge: WebViewJsBridge? = null
    override fun canGoBack() = true
    override fun canGoForward() = false
    override fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>) { events += url }
    override suspend fun loadHtml(html: String?, baseUrl: String?, mimeType: String?, encoding: String?, historyUrl: String?) = Unit
    override suspend fun loadHtmlFile(fileName: String, readType: WebViewFileReadType) = Unit
    override fun postUrl(url: String, postData: ByteArray) = Unit
    override fun goBack() { events += "back" }
    override fun goForward() = Unit
    override fun reload() = Unit
    override fun stopLoading() = Unit
    override fun evaluateJavaScript(script: String, callback: ((String) -> Unit)?) = Unit
    override fun initJsBridge(webViewJsBridge: WebViewJsBridge) = Unit
    override fun saveState(): WebViewBundle? = null
    override fun scrollOffset(): Pair<Int, Int> = 0 to 0
}
