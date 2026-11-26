package com.nuvio.app.features.livetv

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Drives the real Xtream provider against a loopback panel so the built request URLs (categories,
 * live streams, EPG) are asserted, not just the parsed result. There is no Xtream HTTP seam.
 */
class LiveTvXtreamRequestUrlTest {

    @Test
    fun `categories and streams go to the panel player_api with the encoded credentials`() = runBlocking {
        val requests = java.util.Collections.synchronizedList(mutableListOf<String>())
        val server = panel(requests)
        server.start()
        try {
            val base = "http://127.0.0.1:${server.address.port}"
            val settings = LiveTvXtreamSettings(
                serverUrl = "$base/player_api.php/",
                username = "user name",
                password = "p@ss",
            )

            val channel = fetchXtreamChannels(settings).single()

            assertEquals(
                listOf("get_live_categories", null, "get_live_streams"),
                requests.map { queryParameter(it, "action") },
            )
            requests.forEach { request ->
                assertTrue(request.startsWith("/player_api.php?"), request)
                assertTrue("username=user%20name" in request, request)
                assertTrue("password=p%40ss" in request, request)
            }

            assertEquals("xtream:5", channel.id)
            assertEquals("Match", channel.name)
            assertEquals("Sport", channel.group)
            assertEquals("m1", channel.epgId)
            // The panel's own live link, with the account's URL-encoded credentials.
            assertEquals("$base/live/user%20name/p%40ss/5.m3u8", channel.streamUrl)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `the xtream epg url points at xmltv with the encoded credentials`() {
        val settings = LiveTvXtreamSettings(
            serverUrl = "http://panel.example/player_api.php/",
            username = "user name",
            password = "p@ss",
        )

        assertEquals(
            "http://panel.example/xmltv.php?username=user%20name&password=p%40ss",
            xtreamEpgUrl(settings),
        )
    }

    private fun panel(requests: MutableList<String>): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val request = exchange.requestURI.toString()
            requests += request
            val body = when (queryParameter(request, "action")) {
                "get_live_categories" -> """[{"category_id":"2","category_name":"Sport"}]"""
                "get_live_streams" ->
                    """[{"name":"Match","stream_id":"5","category_id":"2","stream_icon":"http://logo","epg_channel_id":"m1"}]"""
                else -> """{"user_info":{"allowed_output_formats":["m3u8"]}}"""
            }
            val bytes = body.encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        return server
    }

    private fun queryParameter(uri: String, name: String): String? =
        uri.substringAfter('?', missingDelimiterValue = "")
            .split('&')
            .mapNotNull { parameter ->
                val key = parameter.substringBefore('=', missingDelimiterValue = "")
                if (key != name) null else parameter.substringAfter('=', missingDelimiterValue = "")
            }
            .firstOrNull()
}
