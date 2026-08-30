package fjallkartan.fjallkartan

import android.content.res.Configuration
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import fjallkartan.fjallkartan.R
import java.util.Locale
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductUiInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun exposesPrimaryMapControlsToAccessibility() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertTrue(device.wait(Until.hasObject(By.desc("Search places")), 5_000))
            assertTrue(device.hasObject(By.desc("Measure distance")))
            assertTrue(device.hasObject(By.desc("More map tools")))
        }
    }

    @Test
    fun importedCatalogueContainsEverySupportedLanguage() {
        val defaultAbout = context.getString(R.string.about)
        listOf("da", "de", "es", "fi", "fr", "it", "nb", "nl", "sv").forEach { language ->
            assertNotEquals(language, defaultAbout, localizedString(Locale(language), R.string.about))
        }
        assertNotEquals(defaultAbout, localizedString(Locale.forLanguageTag("zh-Hans"), R.string.about))
    }

    private fun localizedString(locale: Locale, resId: Int): String {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration).resources.getString(resId)
    }
}

