package com.nuvio.app.features.livetv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveTvEpgParserTest {

    private val noon = 1_794_916_800_000L

    @Test
    fun `parses xmltv times with and without an offset`() {
        assertEquals(noon, parseXmltvTime("20261117120000 +0000"))
        assertEquals(noon, parseXmltvTime("20261117140000 +0200"))
        assertEquals(noon, parseXmltvTime("20261117070000 -0500"))
        assertEquals(noon, parseXmltvTime("20261117120000"))
        assertEquals(noon + 30_000L, parseXmltvTime("20261117120030 +0000"))

        assertNull(parseXmltvTime("20261117"))
        assertNull(parseXmltvTime(""))
        assertNull(parseXmltvTime("not-a-time"))
        assertNull(parseXmltvTime("20261317120000 +0000"))
    }

    @Test
    fun `parses channels programmes and the window, including an empty channel`() {
        val guide = parseXmltvGuide(
            xml = FIXTURE,
            windowStartMillis = noon - LIVE_TV_EPG_PAST_WINDOW_MILLIS,
            windowEndMillis = noon + LIVE_TV_EPG_FUTURE_WINDOW_MILLIS,
        )

        assertEquals(setOf("bbc1.uk"), guide.programsByEpgKey.keys)
        assertEquals("bbc1.uk", guide.epgKeysByName["bbcone"])

        val programs = guide.programsByEpgKey.getValue("bbc1.uk")
        assertEquals(listOf("News & Weather", "Later", "Tomorrow"), programs.map { it.title })
        assertEquals("The midday news", programs[0].description)
        assertEquals(noon, programs[0].startMillis)
        assertEquals(noon + 60 * 60_000L, programs[0].endMillis)

        // The empty channel has a display name but no programmes, and must not throw.
        assertTrue(guide.programsByEpgKey.keys.none { it == "empty.chan" })
    }

    @Test
    fun `decompresses a gzipped xmltv payload and parses it`() {
        val xml = liveTvGunzip(GZIPPED_FIXTURE, maxOutputBytes = 1024 * 1024).decodeToString()

        assertTrue(xml.contains("<title>News &amp; Weather</title>"))

        val guide = parseXmltvGuide(
            xml = xml,
            windowStartMillis = noon - 1_000L,
            windowEndMillis = noon + 1_000L,
        )
        val program = guide.programsByEpgKey.getValue("bbc1.uk").single()
        assertEquals("News & Weather", program.title)
        assertEquals("The midday news", program.description)
        assertEquals(noon, program.startMillis)
        assertEquals(noon + 60 * 60_000L, program.endMillis)
    }

    @Test
    fun `malformed xml yields an empty guide and never throws`() {
        assertTrue(parseXmltvGuide("", noon, noon + 1_000L).programsByEpgKey.isEmpty())
        assertTrue(parseXmltvGuide("<tv><channel id='x'>", noon, noon + 1_000L).programsByEpgKey.isEmpty())
        assertTrue(
            parseXmltvGuide(
                "<tv><programme start='bogus' channel='x'><title>Broken</title></programme></tv>",
                noon,
                noon + 1_000L,
            ).programsByEpgKey.isEmpty(),
        )
    }

    private companion object {
        // Tomorrow is first so the parser has to sort by start time.
        val FIXTURE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="BBC1.uk"><display-name>BBC One</display-name></channel>
              <channel id="empty.chan"><display-name>Empty</display-name></channel>
              <programme start="20261118000000 +0000" stop="20261118010000 +0000" channel="BBC1.uk">
                <title>Tomorrow</title>
              </programme>
              <programme start="20261117150000 +0200" stop="20261117160000 +0200" channel="BBC1.uk">
                <title>Later</title>
              </programme>
              <programme start="20261117120000 +0000" stop="20261117130000 +0000" channel="BBC1.uk">
                <title>News &amp; Weather</title>
                <desc><![CDATA[The midday news]]></desc>
              </programme>
            </tv>
        """.trimIndent()

        // gzip of the same one-programme guide used by the decompression test.
        val GZIPPED_FIXTURE = byteArrayOf(
            31, -117, 8, 0, 0, 0, 0, 0, 2, 3, 85, -112, 59, -113, -62, 48,
            16, -124, 123, 126, -59, -98, 11, -102, 19, 56, -50, 73, 119, 72, 56, 70,
            60, 68, 9, 77, 78, 20, -120, -62, -60, 43, 98, 17, 59, -111, 109, 94,
            -1, 30, 7, 16, 10, 91, 108, -79, -33, -20, 104, 52, 124, 114, 53, 21,
            -100, -47, 121, 93, -37, -116, -80, 97, 66, 0, 109, 81, 43, 109, 15, 25,
            -7, -49, -105, -125, 17, -103, -120, 30, 15, 103, -47, 3, -32, 69, 41, -83,
            -59, 10, -76, -54, -56, 126, 95, -80, -31, -23, 72, 4, 87, -38, 55, -107,
            -68, 13, -84, 52, 40, 102, -77, 57, -84, 45, 114, -6, 113, -27, -12, -11,
            -6, -80, 105, 92, 125, 112, -46, 24, 4, 31, -92, 11, 25, 73, -109, -12,
            -105, 49, -10, -57, -46, 36, 14, 124, -73, -101, 68, 88, 55, 29, -10, -45,
            101, 47, -69, 78, -116, 104, 28, -83, -125, 14, 21, -118, 21, 94, 60, -12,
            -91, 105, -58, -80, 65, 25, 74, 116, -100, 62, -55, 83, -91, -48, 23, -126,
            127, 109, -25, -117, 105, 62, -35, -26, 37, -126, -47, 74, -55, 27, -40, -8,
            -72, -37, -59, -72, 15, 69, -101, -107, -66, -61, -58, 26, 104, -37, -61, 29,
            34, -71, 15, -74, 52, 1, 0, 0,
        )
    }
}
