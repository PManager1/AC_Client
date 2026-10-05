package com.example.birdy

import com.example.birdy.data.Config
import com.example.birdy.data.HomeFDData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFeedParseTest {

    @Test
    fun homeFeedUrlMatchesIos() {
        assertEquals(
            "${Config.API_BASE_URL}/homefeed?category=food&filterByActivePolygons=true&lat=38.9&lng=-77.0&maxDistance=10",
            HomeFDData.homeFeedUrl("Food", 38.9, -77.0)
        )
        assertEquals(
            "${Config.API_BASE_URL}/homefeed?category=all&filterByActivePolygons=true",
            HomeFDData.homeFeedUrl("All", null, null)
        )
    }

    @Test
    fun parsesUdo3HomeFeedShape() {
        // Shape of udo3 HomeFeedJSON (home_feed_json.ex)
        val json = """
            {"featured_banners": [],
             "sections": [{"heading": "Fastest near you", "restaurants": [
               {"id": "b1", "restaurantName": "Taco Spot", "logoURL": null, "images": ["https://x/1.jpg"],
                "rating": 4.5, "reviewCount": 100, "distance": 1.0, "deliveryTime": 20, "deliveryFee": 0.0,
                "promoText": "", "isSponsored": false, "foodItems": [], "isNew": false, "phone": null,
                "tags": ["mexican"], "isBrandItem": true, "isFavorited": true}
             ]}]}
        """.trimIndent()

        val feed = HomeFDData.parseHomeFeed(json)
        val card = feed.sections.single().restaurants.single()

        assertEquals("Fastest near you", feed.sections.single().heading)
        assertEquals("Taco Spot", card.restaurantName)
        assertEquals("", card.logoURL)
        assertEquals("", card.phone)
        assertEquals(listOf("https://x/1.jpg"), card.images)
        assertTrue(card.isBrandItem)
        assertTrue(card.isFavorited)
    }

    @Test
    fun missingSectionsAndBadCardsDoNotFailTheFeed() {
        assertTrue(HomeFDData.parseHomeFeed("""{"featured_banners": []}""").sections.isEmpty())

        val feed = HomeFDData.parseHomeFeed(
            """{"sections": [{"heading": "Most loved", "restaurants": [{"restaurantName": "no id"}, {"id": "b2"}]}]}"""
        )
        val cards = feed.sections.single().restaurants
        assertEquals(listOf("b2"), cards.map { it.id })
        assertFalse(cards.single().isBrandItem)
        assertEquals("", cards.single().restaurantName)
    }
}
