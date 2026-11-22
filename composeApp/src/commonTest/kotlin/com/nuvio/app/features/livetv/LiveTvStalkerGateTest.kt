package com.nuvio.app.features.livetv

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveTvStalkerGateTest {

    @Test
    fun `releases the current stream before creating the next one`() = runTest {
        val order = mutableListOf<String>()
        val gate = gate()

        gate.open("AA", "1", StalkerStage.Preview, { order += "release-1" }) {
            order += "create-1"
            channel("1")
        }
        gate.open("AA", "2", StalkerStage.Full, { order += "release-2" }) {
            order += "create-2"
            channel("2")
        }

        assertEquals(listOf("release-1", "create-1", "release-2", "create-2"), order)
        assertEquals("2", gate.activeChannelId)
        assertEquals(StalkerStage.Full, gate.lastStage)
    }

    @Test
    fun `three attempts with one and two second backoff then the portal is busy`() = runTest {
        val waits = mutableListOf<Long>()
        val logs = mutableListOf<String>()
        val gate = gate(waits = waits, logs = logs)
        val releases = mutableListOf<Int>()
        var attempts = 0

        val failure = runCatching {
            gate.open("AA", "1", StalkerStage.Full, { releases += releases.size + 1 }) {
                attempts++
                error("HTTP 403")
            }
        }

        assertTrue(failure.exceptionOrNull() is StalkerPortalBusy)
        assertEquals(STALKER_BUSY, failure.exceptionOrNull()?.message)
        assertEquals(3, attempts)
        assertEquals(listOf(1_000L, 2_000L), waits)
        // Every attempt is preceded by a release, and no create happens without one.
        assertEquals(listOf(1, 2, 3), releases)
        assertFalse(gate.fullPlayerLoading)
    }

    @Test
    fun `a non auth failure is not retried`() = runTest {
        val waits = mutableListOf<Long>()
        val gate = gate(waits = waits)
        var attempts = 0

        val failure = runCatching {
            gate.open("AA", "1", StalkerStage.Full) {
                attempts++
                error("HTTP 500")
            }
        }

        assertEquals("HTTP 500", failure.exceptionOrNull()?.message)
        assertEquals(1, attempts)
        assertTrue(waits.isEmpty())
        assertFalse(gate.fullPlayerLoading)
    }

    private fun gate(
        waits: MutableList<Long> = mutableListOf(),
        logs: MutableList<String> = mutableListOf(),
    ) = LiveTvStalkerGate().apply {
        pause = { waits += it }
        log = { logs += it }
    }

    private fun channel(id: String) = LiveTvChannel(
        id = id,
        name = "Channel $id",
        streamUrl = "http://cdn/$id.ts",
    )
}
