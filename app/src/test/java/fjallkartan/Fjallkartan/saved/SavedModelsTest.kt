package fjallkartan.fjallkartan.saved

import fjallkartan.fjallkartan.measurement.GeoCoordinate
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedModelsTest {
    @Test
    fun routeRoundTripsUsingIosCoordinateKeys() {
        val route = SavedRoute(
            id = UUID.fromString("11111111-2222-3333-4444-555555555555"),
            createdAt = Instant.parse("2026-08-25T12:34:56Z"),
            meters = 1234.0,
            coordinates = listOf(GeoCoordinate(67.0, 16.0), GeoCoordinate(67.1, 16.1)),
            strokeSizes = listOf(2),
            ascent = 45.0,
            descent = 30.0,
            elevations = listOf(100.0, null, 120.0),
            name = "Route",
        )

        val json = route.toJson()
        assertEquals(67.0, json.getJSONArray("coordinates").getJSONObject(0).getDouble("latitude"), 0.0)
        assertEquals(route, SavedRoute.fromJson(json))
    }

    @Test
    fun pinRoundTrips() {
        val pin = SavedPin(coordinate = GeoCoordinate(68.0, 18.0), name = "Cabin")
        assertEquals(pin, SavedPin.fromJson(pin.toJson()))
    }
}
