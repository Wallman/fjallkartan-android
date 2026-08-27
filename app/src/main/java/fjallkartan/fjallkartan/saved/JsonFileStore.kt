package fjallkartan.fjallkartan.saved

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.util.UUID
import org.json.JSONObject

class JsonFileStore<T>(
    context: Context,
    directoryName: String,
    private val encode: (T) -> JSONObject,
    private val decode: (JSONObject) -> T,
    private val identifier: (T) -> UUID,
) {
    private val directory = File(context.filesDir, directoryName).apply { mkdirs() }

    fun load(): List<T> {
        return directory.listFiles()
            ?.asSequence()
            ?.filter { it.extension == "json" }
            ?.mapNotNull { file ->
                runCatching { decode(JSONObject(file.readText())) }.getOrNull()
            }
            ?.toList()
            ?: emptyList()
    }

    fun save(item: T) {
        val atomic = AtomicFile(file(identifier(item)))
        val stream = atomic.startWrite()
        try {
            stream.write(encode(item).toString().toByteArray())
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    }

    fun delete(id: UUID) {
        file(id).delete()
    }

    private fun file(id: UUID): File = File(directory, "$id.json")
}
