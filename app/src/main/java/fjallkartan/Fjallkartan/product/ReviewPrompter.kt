package fjallkartan.fjallkartan.product

import android.content.Context
import fjallkartan.fjallkartan.BuildConfig

class ReviewPrompter(
    context: Context,
    private val now: () -> Long = System::currentTimeMillis,
    private val version: String = BuildConfig.VERSION_NAME,
) {
    private val preferences = context.getSharedPreferences("review-prompter", Context.MODE_PRIVATE)

    fun noteAppOpen() {
        preferences.edit().putInt(APP_OPENS, preferences.getInt(APP_OPENS, 0) + 1).apply()
    }

    fun recordMeasurement(meters: Double): Boolean {
        if (meters < MINIMUM_MEASUREMENT_METERS) return false
        preferences.edit()
            .putInt(MEASUREMENTS, preferences.getInt(MEASUREMENTS, 0) + 1)
            .apply()
        return isEligible()
    }

    fun recordOfflineCompletion(): Boolean {
        preferences.edit()
            .putInt(DOWNLOADS, preferences.getInt(DOWNLOADS, 0) + 1)
            .apply()
        return isEligible()
    }

    fun consume(): Boolean {
        if (!isEligible()) return false
        preferences.edit()
            .putString(PROMPTED_VERSION, version)
            .putLong(PROMPTED_AT, now())
            .apply()
        return true
    }

    internal fun isEligible(): Boolean {
        if (preferences.getInt(APP_OPENS, 0) < REQUIRED_APP_OPENS) return false
        val succeeded = preferences.getInt(DOWNLOADS, 0) > 0 ||
            preferences.getInt(MEASUREMENTS, 0) >= REQUIRED_MEASUREMENTS
        if (!succeeded || preferences.getString(PROMPTED_VERSION, null) == version) return false
        val promptedAt = preferences.getLong(PROMPTED_AT, 0)
        return promptedAt == 0L || now() - promptedAt >= MINIMUM_PROMPT_INTERVAL_MILLIS
    }

    companion object {
        const val MINIMUM_MEASUREMENT_METERS = 500.0
        private const val REQUIRED_APP_OPENS = 3
        private const val REQUIRED_MEASUREMENTS = 3
        private const val MINIMUM_PROMPT_INTERVAL_MILLIS = 120L * 24 * 60 * 60 * 1_000
        private const val APP_OPENS = "app-opens"
        private const val DOWNLOADS = "downloads"
        private const val MEASUREMENTS = "measurements"
        private const val PROMPTED_VERSION = "prompted-version"
        private const val PROMPTED_AT = "prompted-at"
    }
}
