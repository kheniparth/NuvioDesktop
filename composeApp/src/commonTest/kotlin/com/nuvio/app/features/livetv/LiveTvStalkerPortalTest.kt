package com.nuvio.app.features.livetv

import com.nuvio.app.features.addons.RawHttpResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveTvStalkerPortalTest {

    private var previousHttpGet: (suspend (String, Map<String, String>) -> RawHttpResponse)? = null

    private val settings = LiveTvStalkerSettings(
        portalUrl = "http://portal.example/c/",
        macAddress = "00:1a:79:11:22:33",
    )

    @BeforeTest
    fun captureSeam() {
        previousHttpGet = stalkerHttpGet
        clearCachedStalkerSession()
    }

    @AfterTest
    fun restoreSeam() {
        previousHttpGet?.let { stalkerHttpGet = it }
        previousHttpGet = null
        clearCachedStalkerSession()
    }

    @Test
    fun `401 with a token re-handshakes exactly once and retries with the new token`() = runTest {
        val calls = mutableListOf<String>()
        var firstGenres = true
        stalkerHttpGet = { url, _ ->
            calls += url
            when {
                "action=handshake" in url -> response(
                    url = url,
                    body = """{"js":{"token":"token${calls.count { "action=handshake" in it }}"}}""",
                )
                "action=get_genres" in url && firstGenres -> {
                    firstGenres = false
                    response(url = url, status = 401, body = "")
                }
                "action=get_genres" in url -> response(url = url, body = """{"js":{"data":[]}}""")
                "action=get_ordered_list" in url -> response(url = url, body = """{"js":{"data":[]}}""")
                else -> error("unexpected request $url")
            }
        }

        assertTrue(fetchStalkerChannels(settings).isEmpty())

        assertEquals(2, calls.count { "action=handshake" in it })
        val genresCalls = calls.filter { "action=get_genres" in it }
        assertEquals(2, genresCalls.size)
        assertTrue("token=token1" in genresCalls[0])
        assertTrue("token=token2" in genresCalls[1])
        // The refreshed session is kept: a second load must not handshake again.
        fetchStalkerChannels(settings)
        assertEquals(2, calls.count { "action=handshake" in it })
    }

    @Test
    fun `a failed refresh surfaces the http failure instead of looping`() = runTest {
        val calls = mutableListOf<String>()
        stalkerHttpGet = { url, _ ->
            calls += url
            when {
                "action=handshake" in url -> response(
                    url = url,
                    body = """{"js":{"token":"token${calls.count { "action=handshake" in it }}"}}""",
                )
                "action=get_genres" in url -> response(url = url, status = 401, body = "")
                else -> error("unexpected request $url")
            }
        }

        val failure = runCatching { fetchStalkerChannels(settings) }

        assertTrue(failure.isFailure)
        assertEquals("HTTP 401", failure.exceptionOrNull()?.message)
        assertEquals(2, calls.count { "action=handshake" in it })
        assertEquals(2, calls.count { "action=get_genres" in it })
    }

    @Test
    fun `a 401 on the initial handshake surfaces the failure without a retry`() = runTest {
        val calls = mutableListOf<String>()
        stalkerHttpGet = { url, _ ->
            calls += url
            response(url = url, status = 401, body = "")
        }

        val failure = runCatching { fetchStalkerChannels(settings) }

        assertTrue(failure.isFailure)
        assertEquals("HTTP 401", failure.exceptionOrNull()?.message)
        assertEquals(1, calls.size)
        assertTrue("action=handshake" in calls.single())
    }

    private fun response(url: String, status: Int = 200, body: String): RawHttpResponse =
        RawHttpResponse(
            status = status,
            statusText = if (status == 200) "OK" else "Unauthorized",
            url = url,
            body = body,
            headers = emptyMap(),
        )
}
