package com.example.birdy

import com.example.birdy.data.SearchApi
import com.example.birdy.data.SearchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchApiTest {

    @Test
    fun cleanQueryTrimsAndCollapsesWhitespace() {
        assertEquals("piz za", SearchApi.cleanQuery("  piz \t  za  "))
        assertEquals("", SearchApi.cleanQuery("   "))
        assertEquals("chick fil a", SearchApi.cleanQuery("chick fil a"))
    }

    @Test
    fun buildQueryEncodesPlusAndSpecialCharacters() {
        assertEquals("include=brands&q=snack%2Bwrap", SearchApi.buildQuery(linkedMapOf("include" to "brands", "q" to "snack+wrap")))
        assertEquals("q=a+%26+b%23", SearchApi.buildQuery(mapOf("q" to "a & b#")))
    }

    @Test
    fun parseBrandsToleratesNullAndMissingFields() {
        val json = """
            {"query": "pizza", "brands": [
              {"id": "1", "name": "Pizza Hut", "logoUrl": null, "tags": null, "brandType": "restaurant"},
              {"id": "2", "name": null, "brandType": null},
              {"name": "no id, skipped"},
              "not an object"
            ]}
        """.trimIndent()

        val result = SearchApi.parseBrands(json)

        assertEquals("pizza", result.query)
        assertEquals(listOf("1", "2"), result.brands.map { it.id })
        assertEquals("", result.brands[0].logoUrl)
        assertEquals(emptyList<String>(), result.brands[0].tags)
        assertEquals("", result.brands[1].name)
        assertEquals("restaurant", result.brands[0].brandType)
        assertEquals("", result.brands[1].brandType)
    }

    @Test
    fun parseHistoryToleratesNullAndMissingFields() {
        val json = """
            {"query": "",
             "recentSearches": [{"query": "Pizza", "count": 3}, {"query": null}, {"count": 2}],
             "recentlyVisitedBrands": [
               {"brandId": "b1", "brandName": "Taco Spot", "logoUrl": null, "tags": ["mexican"], "visitedAt": "2026-10-02T14:03:11Z", "brandType": "grocery"},
               {"brandId": "b2", "visitedAt": null}
             ]}
        """.trimIndent()

        val result = SearchApi.parseHistory(json)

        assertEquals(SearchStatus.SUCCESS, result.status)
        assertEquals(listOf("Pizza"), result.recentSearches.map { it.query })
        assertEquals(3, result.recentSearches[0].count)
        assertEquals(listOf("b1", "b2"), result.visitedBrands.map { it.brandId })
        assertEquals("", result.visitedBrands[0].logoUrl)
        assertEquals(listOf("mexican"), result.visitedBrands[0].tags)
        assertEquals("2026-10-02T14:03:11Z", result.visitedBrands[0].visitedAt)
        assertEquals("", result.visitedBrands[1].brandName)
        assertNull(result.visitedBrands[1].visitedAt)
        assertEquals("grocery", result.visitedBrands[0].brandType)
        assertEquals("", result.visitedBrands[1].brandType)
    }

    @Test
    fun isGroceryTypeMatchesGroceryOnlyIgnoringCase() {
        assertTrue(SearchApi.isGroceryType("grocery"))
        assertTrue(SearchApi.isGroceryType("Grocery"))
        assertFalse(SearchApi.isGroceryType("restaurant"))
        assertFalse(SearchApi.isGroceryType(""))
    }

    @Test
    fun parseHistoryWithOnlyBrandsSectionReturnsEmptySearches() {
        val result = SearchApi.parseHistory("""{"query": "x", "brands": []}""")
        assertEquals(emptyList<Any>(), result.recentSearches)
        assertEquals(emptyList<Any>(), result.visitedBrands)
    }
}
