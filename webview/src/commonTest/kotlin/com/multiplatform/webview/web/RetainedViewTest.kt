package com.multiplatform.webview.web

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class RetainedViewTest {
    @Test
    fun navigationAwayAndBackReusesViewAndFinalPopReleasesOnce() {
        val retained = RetainedView<Any>()
        var creations = 0
        var releases = 0
        fun attach() = retained.attach(create = { creations++; Any() }, release = { releases++ })
        val first = attach()
        retained.detach(first)
        assertSame(first, attach())
        retained.detach(first)
        retained.dispose()
        retained.dispose()
        assertEquals(1, creations)
        assertEquals(1, releases)
        assertNull(retained.value)
    }

    @Test
    fun popWhileMountedDefersDestructionUntilInteropRelease() {
        val retained = RetainedView<Any>()
        var releases = 0
        val view = retained.attach({ Any() }, { releases++ })
        retained.dispose()
        assertEquals(0, releases)
        retained.detach(view)
        retained.detach(view)
        assertEquals(1, releases)
        assertFailsWith<IllegalStateException> { retained.attach({ Any() }, {}) }
    }

    @Test
    fun hostRecreationEvictsOldContextWithoutDisposingRouteState() {
        val retained = RetainedView<Any>()
        var releases = 0
        val old = retained.attach({ Any() }, { releases++ })
        retained.evict(old)
        val replacement = retained.attach({ Any() }, { releases++ })
        retained.detach(old) // delayed callback from the old Activity
        retained.evict(old)
        assertSame(replacement, retained.value)
        assertNotSame(old, replacement)
        assertEquals(1, releases)
        retained.detach(replacement)
        retained.dispose()
        assertEquals(2, releases)
    }

    @Test
    fun differentEntriesNeverShareViewsAndOneEntryCannotMountTwice() {
        val first = RetainedView<Any>()
        val second = RetainedView<Any>()
        assertNotSame(first.attach({ Any() }, {}), second.attach({ Any() }, {}))
        assertFailsWith<IllegalStateException> { first.attach({ Any() }, {}) }
    }

    @Test
    fun effectRestartSkipsSameRequestButNewUrlAndNewNativeViewLoad() = runTest {
        val loads = WebViewContentLoadState<Any>()
        val view = Any()
        val requests = mutableListOf<String>()
        suspend fun load(native: Any, url: String) = loads.load(native, WebContent.Url(url)) { requests += url }
        load(view, "https://example.test/a")
        load(view, "https://example.test/a") // collector restarted on return
        load(view, "https://example.test/b")
        load(view, "https://example.test/a") // explicit URL changed back
        load(Any(), "https://example.test/a") // fresh renderer must not stay blank
        assertEquals(listOf("https://example.test/a", "https://example.test/b", "https://example.test/a", "https://example.test/a"), requests)
    }

    @Test
    fun cancelledLoadCanRetryAndNavigatorOnlyDoesNotSubmitContent() = runTest {
        val loads = WebViewContentLoadState<Any>()
        val view = Any()
        val content = WebContent.File("file:///support/index.html", WebViewFileReadType.COMPOSE_RESOURCE_FILES)
        assertFailsWith<CancellationException> {
            loads.load(view, content) { throw CancellationException("left composition") }
        }
        var submissions = 0
        loads.load(view, content) { submissions++ }
        loads.load(view, content) { submissions++ }
        loads.load(view, WebContent.NavigatorOnly) { submissions++ }
        assertEquals(1, submissions)
    }
}
