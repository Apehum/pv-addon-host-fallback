package su.plo.voice.hostfallback.store

import java.util.UUID

interface PlayerHostStore {
    fun load(): Map<UUID, String>

    fun save(data: Map<UUID, String>)
}
