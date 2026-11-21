package com.nuvio.app.features.livetv

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.util.zip.GZIPInputStream

// Store and key names match LiveTvStorage.android.kt, so scripts/setup-test-env.sh writes both the same way.
actual object LiveTvStorage {
    private val store = DesktopStorage.store("nuvio_live_tv")
    private const val separator = '\u001F'

    private fun get(key: String): String? = store.getString(ProfileScopedKey.of(key))
    private fun put(key: String, value: String) = store.putString(ProfileScopedKey.of(key), value)

    actual fun loadPlaylistUrl(): String? = get("playlist_url")
    actual fun savePlaylistUrl(url: String) = put("playlist_url", url)
    actual fun loadPlaylistsBlob(): String? = get("playlists_blob")
    actual fun savePlaylistsBlob(blob: String) = put("playlists_blob", blob)
    actual fun loadFavoriteChannelIdsBlob(): String? = get("favorite_channel_ids_blob")
    actual fun saveFavoriteChannelIdsBlob(blob: String) = put("favorite_channel_ids_blob", blob)
    actual fun loadLastWatchedChannelId(): String? = get("last_watched_channel_id")
    actual fun saveLastWatchedChannelId(channelId: String) = put("last_watched_channel_id", channelId)
    actual fun loadRecentChannelIdsBlob(): String? = get("recent_channel_ids")
    actual fun saveRecentChannelIdsBlob(blob: String) = put("recent_channel_ids", blob)
    actual fun loadChannelViewMode(): String? = get("channel_view_mode")
    actual fun saveChannelViewMode(mode: String) = put("channel_view_mode", mode)
    actual fun loadNavigationEnabled(): Boolean? = get("navigation_enabled")?.toBooleanStrictOrNull()
    actual fun saveNavigationEnabled(enabled: Boolean) = put("navigation_enabled", enabled.toString())

    actual fun loadStalkerSettings(): LiveTvStalkerSettings =
        get("stalker_settings")?.split(separator)?.let {
            LiveTvStalkerSettings(
                it.getOrNull(0).orEmpty(), it.getOrNull(1).orEmpty(), it.getOrNull(2).orEmpty(),
                it.getOrNull(3).orEmpty(), it.getOrNull(4) != "false",
            )
        } ?: LiveTvStalkerSettings()

    actual fun saveStalkerSettings(settings: LiveTvStalkerSettings) = put(
        "stalker_settings",
        listOf(settings.portalUrl, settings.macAddress, settings.username, settings.password, settings.isEnabled)
            .joinToString(separator.toString()),
    )

    actual fun loadXtreamSettings(): LiveTvXtreamSettings =
        get("xtream_settings")?.split(separator)?.let {
            LiveTvXtreamSettings(
                it.getOrNull(0).orEmpty(), it.getOrNull(1).orEmpty(), it.getOrNull(2).orEmpty(),
                it.getOrNull(3) != "false",
            )
        } ?: LiveTvXtreamSettings()

    actual fun saveXtreamSettings(settings: LiveTvXtreamSettings) = put(
        "xtream_settings",
        listOf(settings.serverUrl, settings.username, settings.password, settings.isEnabled)
            .joinToString(separator.toString()),
    )

    actual fun publishNavigationVisibility(visible: Boolean) = Unit
    actual fun loadEpgSourcesBlob(): String? = get("epg_sources_blob")
    actual fun saveEpgSourcesBlob(blob: String) = put("epg_sources_blob", blob)
    actual fun loadEpgAutoDetectEnabled(): Boolean? = get("epg_auto_detect_enabled")?.toBooleanStrictOrNull()
    actual fun saveEpgAutoDetectEnabled(enabled: Boolean) = put("epg_auto_detect_enabled", enabled.toString())
}

internal actual fun liveTvGunzip(bytes: ByteArray, maxOutputBytes: Int): ByteArray {
    val output = ByteArrayOutputStream(minOf(maxOf(bytes.size * 4, 64 * 1024), maxOutputBytes))
    GZIPInputStream(ByteArrayInputStream(bytes), 64 * 1024).use { input ->
        val buffer = ByteArray(64 * 1024)
        var total = 0
        try {
            while (total < maxOutputBytes) {
                val read = input.read(buffer, 0, minOf(buffer.size, maxOutputBytes - total))
                if (read <= 0) break
                output.write(buffer, 0, read)
                total += read
            }
        } catch (_: EOFException) {
        }
    }
    return output.toByteArray()
}
