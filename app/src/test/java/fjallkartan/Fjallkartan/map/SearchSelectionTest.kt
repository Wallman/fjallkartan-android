package fjallkartan.fjallkartan.map

import fjallkartan.fjallkartan.measurement.GeoCoordinate
import fjallkartan.fjallkartan.search.PlaceResult
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchSelectionTest {
    @Test
    fun tokenDistinguishesRepeatedSelectionOfSamePlace() {
        val place = PlaceResult(
            id = 1,
            name = "Test",
            kind = 0,
            matchedAlias = null,
            municipality = null,
            region = null,
            country = 0,
            coordinate = GeoCoordinate(67.0, 18.0),
        )

        assertEquals(SearchSelection(place, 1).place, SearchSelection(place, 2).place)
        assertEquals(1, SearchSelection(place, 1).token)
        assertEquals(2, SearchSelection(place, 2).token)
    }
}
