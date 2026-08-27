package fjallkartan.fjallkartan.search

import fjallkartan.fjallkartan.measurement.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceSearchTest {
    @Test
    fun buildsSafePrefixExpression() {
        assertEquals("abisko* AND turist*", PlaceSearch.ftsExpression("Abisko turist"))
        assertEquals("idnetčohkka* AND or*", PlaceSearch.ftsExpression("\"Idnetčohkka\" OR *"))
        assertNull(PlaceSearch.ftsExpression(" — "))
    }

    @Test
    fun formatsCountryCodesLikeIos() {
        fun result(country: Int) = PlaceResult(
            id = 1,
            name = "Place",
            kind = 0,
            matchedAlias = null,
            municipality = null,
            region = null,
            country = country,
            coordinate = GeoCoordinate(0.0, 0.0),
        )

        assertEquals("SE", result(0).countryCode)
        assertEquals("NO", result(1).countryCode)
    }
}
