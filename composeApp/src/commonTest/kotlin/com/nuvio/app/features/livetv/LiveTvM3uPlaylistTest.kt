package com.nuvio.app.features.livetv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveTvM3uPlaylistTest {

    @Test
    fun `parses tvg attributes and a channel without tvg-id`() {
        val playlist = LiveTvPlaylist(
            id = "p1",
            name = "Mine",
            type = LiveTvPlaylistType.Url,
            source = "http://example/pl.m3u",
        )
        val payload = """
            #EXTM3U url-tvg="http://epg.example/guide.xml"
            #EXTINF:-1 tvg-id="bbc1" tvg-name="BBC One" tvg-logo="http://logo/bbc.png" group-title="News, UK",BBC One HD
            http://stream.example/bbc.ts
            #EXTINF:-1 tvg-name="No Id" tvg-logo="http://logo/noid.png",Channel Without Id
            http://stream.example/noid.ts
            #EXTINF:-1,Plain
            http://stream.example/plain.ts
        """.trimIndent().replace("\n", "\r\n")

        val channels = parseM3uPlaylist(payload, playlist)

        assertEquals(3, channels.size)
        assertEquals(
            listOf("BBC One HD", "Channel Without Id", "Plain"),
            channels.map { it.name },
        )
        channels.forEach {
            assertEquals("p1", it.playlistId)
            assertEquals("Mine", it.playlistName)
        }

        val withId = channels[0]
        assertEquals("bbc1", withId.epgId)
        assertEquals("http://logo/bbc.png", withId.logoUrl)
        assertEquals("News, UK", withId.group)
        assertEquals("http://stream.example/bbc.ts", withId.streamUrl)

        // A missing tvg-id is the #39 case: the channel still parses, with no EPG id.
        val missingId = channels[1]
        assertNull(missingId.epgId)
        assertEquals("Channel Without Id", missingId.name)
        assertEquals("http://logo/noid.png", missingId.logoUrl)
        assertEquals("http://stream.example/noid.ts", missingId.streamUrl)

        val plain = channels[2]
        assertNull(plain.epgId)
        assertNull(plain.group)
        assertNull(plain.logoUrl)
        assertEquals(mapOf("User-Agent" to LIVE_TV_STREAM_USER_AGENT), plain.headers)
        assertEquals(3, channels.map { it.id }.distinct().size)
    }

    @Test
    fun `reads playlist headers vlc options and per-url headers`() {
        val payload = """
            #EXTM3U
            #EXTINF:-1 http-user-agent="Agent/1" http-referrer="http://ref.example/",Header Channel
            http://stream.example/headers.ts
            #EXTINF:-1 tvg-id="vlc",Vlc Channel
            #EXTVLCOPT:http-user-agent=VlcAgent/1
            #EXTVLCOPT:http-referer=http://vlc.example/
            http://stream.example/vlc.ts
            #EXTINF:-1 tvg-id="query",Query Channel
            http://stream.example/query.ts|User-Agent=Query%20Agent&Referer=http%3A%2F%2Fquery.example%2F
        """.trimIndent()

        val channels = parseM3uPlaylist(payload)

        assertEquals("Agent/1", channels[0].headers["User-Agent"])
        assertEquals("http://ref.example/", channels[0].headers["Referer"])
        assertEquals("VlcAgent/1", channels[1].headers["User-Agent"])
        assertEquals("http://vlc.example/", channels[1].headers["Referer"])
        assertEquals("Query Agent", channels[2].headers["User-Agent"])
        assertEquals("http://query.example/", channels[2].headers["Referer"])
        assertEquals("http://stream.example/query.ts", channels[2].streamUrl)
    }

    @Test
    fun `extracts epg guide urls from the header`() {
        val urls = parseM3uEpgUrls(
            """
            #EXTM3U x-tvg-url="http://epg.example/a.xml,http://epg.example/b.xml.gz" url-tvg="http://epg.example/b.xml.gz"
            #EXTINF:-1,One
            http://stream.example/one.ts
            """.trimIndent(),
        )

        assertEquals(
            // url-tvg is read first, then x-tvg-url; duplicates are dropped.
            listOf("http://epg.example/b.xml.gz", "http://epg.example/a.xml"),
            urls,
        )
    }

    @Test
    fun `empty or malformed playlists return nothing and never throw`() {
        assertTrue(parseM3uPlaylist("").isEmpty())
        assertTrue(parseM3uPlaylist("   \n\n  ").isEmpty())
        assertTrue(parseM3uEpgUrls("").isEmpty())
        assertTrue(parseM3uEpgUrls("not a playlist").isEmpty())

        // Unterminated and attribute-only entries produce no channel, but no crash either.
        val malformed = """
            #EXTM3U
            #EXTINF:-1 tvg-id="unterminated
            #EXTINF:-1 tvg-logo="http://logo/x.png"
        """.trimIndent()
        assertTrue(parseM3uPlaylist(malformed).isEmpty())
    }
}
