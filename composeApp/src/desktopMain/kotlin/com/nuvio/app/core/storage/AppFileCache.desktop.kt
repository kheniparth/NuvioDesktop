package com.nuvio.app.core.storage

import java.io.File

internal actual object AppFileCache {
    private val root: File by lazy { DesktopStorage.cacheDir.resolve("nuvio_file_cache").toFile() }

    actual fun read(path: String): String? =
        root.resolve(path).takeIf { it.isFile && it.length() > 0L }?.let { runCatching { it.readText() }.getOrNull() }

    actual fun write(path: String, payload: String) {
        val target = root.resolve(path)
        val directory = target.parentFile ?: return
        directory.mkdirs()
        val temporary = directory.resolve(".${target.name}.tmp")
        temporary.writeText(payload)
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }
}
