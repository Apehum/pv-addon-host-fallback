package su.plo.voice.hostfallback.store

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

class JsonPlayerHostStore(
    private val file: File,
) : PlayerHostStore {
    private val gson = Gson()

    private val mapType = object : TypeToken<Map<UUID, String>>() {}.type

    override fun load(): Map<UUID, String> {
        if (!file.exists()) {
            return emptyMap()
        }

        return try {
            gson.fromJson(file.readText(), mapType)
        } catch (e: Throwable) {
            throw RuntimeException("Failed to parse json", e)
        }
    }

    override fun save(data: Map<UUID, String>) {
        if (data.isEmpty() && file.exists()) {
            file.delete()
            return
        }

        val json = gson.toJson(data)
        file.writeText(json)
    }
}
