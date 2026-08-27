package fjallkartan.fjallkartan.product

import android.content.Context
import org.json.JSONObject
import java.util.Locale

class Localizer private constructor(private val translations: JSONObject) {
    fun text(key: String, locale: Locale): String {
        val language = when {
            locale.language == "zh" -> "zh-Hans"
            else -> locale.language
        }
        return translations.optJSONObject(language)?.optString(key).orEmpty().ifBlank { key }
    }

    companion object {
        @Volatile private var shared: Localizer? = null

        fun get(context: Context): Localizer = shared ?: synchronized(this) {
            shared ?: Localizer(
                JSONObject(context.assets.open("localizations.json").bufferedReader().use { it.readText() }),
            ).also { shared = it }
        }
    }
}
