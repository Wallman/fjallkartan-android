package fjallkartan.fjallkartan

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import fjallkartan.fjallkartan.product.Localizer
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
        val localizer = Localizer.get(context)
        listOf("da", "es", "fi", "fr", "it", "nb", "nl", "sv").forEach { language ->
            assertNotEquals("About", localizer.text("About", Locale(language)))
        }
        assertNotEquals("About", localizer.text("About", Locale.GERMAN))
        assertNotEquals("About", localizer.text("About", Locale.forLanguageTag("zh-Hans")))
    }
}
