package fjallkartan.fjallkartan

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fjallkartan.fjallkartan.measurement.GeoCoordinate
import fjallkartan.fjallkartan.saved.FeaturedRoutes
import fjallkartan.fjallkartan.saved.JsonFileStore
import fjallkartan.fjallkartan.saved.SavedPin
import fjallkartan.fjallkartan.search.PlaceSearch
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchAndSavedInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun searchesBundledFtsIndex() {
        val results = PlaceSearch(context).search("Abisko")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name.contains("Abisko", ignoreCase = true) })
    }

    @Test
    fun loadsFeaturedRoutes() {
        val routes = FeaturedRoutes.load(context)
        assertTrue(routes.size >= 8)
        assertTrue(routes.all { it.route.coordinates.size >= 2 })
    }

    @Test
    fun atomicallyPersistsPins() {
        val directory = "test-pins-${System.nanoTime()}"
        val store = JsonFileStore(
            context,
            directory,
            SavedPin::toJson,
            SavedPin::fromJson,
            SavedPin::id,
        )
        val pin = SavedPin(coordinate = GeoCoordinate(67.0, 16.0), name = "Test")
        store.save(pin)
        assertEquals(listOf(pin), store.load())
        store.delete(pin.id)
        assertTrue(store.load().isEmpty())
        File(context.filesDir, directory).delete()
    }
}
