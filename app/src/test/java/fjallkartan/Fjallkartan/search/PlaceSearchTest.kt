package fjallkartan.fjallkartan.search

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
}
