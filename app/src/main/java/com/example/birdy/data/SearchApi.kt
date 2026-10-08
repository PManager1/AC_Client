package com.example.birdy.data

import com.example.birdy.data.Config.API_BASE_URL
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Matches iOS SearchAPI (SearchFood.swift) — the udo3 search API:
//   GET    /search?include=history            recent searches + recently visited stores
//   GET    /search?include=brands&q=...       brand typeahead
//   POST   /search/history                    {"query"} and/or {"brandId"}
//   DELETE /search/history?type=searches|brands|all

data class BrandSuggestion(
    val id: String,
    val name: String,
    val logoUrl: String,
    val tags: List<String>,
    val brandType: String = ""
)

data class RecentSearchEntry(
    val query: String,
    val count: Int
)

data class VisitedBrand(
    val brandId: String,
    val brandName: String,
    val logoUrl: String,
    val tags: List<String>,
    val visitedAt: String? = null,
    val brandType: String = ""
)

enum class SearchStatus { SUCCESS, AUTH_ERROR, NETWORK_ERROR }

data class SearchHistoryResult(
    val status: SearchStatus,
    val recentSearches: List<RecentSearchEntry> = emptyList(),
    val visitedBrands: List<VisitedBrand> = emptyList()
)

data class BrandSearchResult(
    val query: String,
    val brands: List<BrandSuggestion>
)

/** All calls are blocking — run them from Dispatchers.IO. */
object SearchApi {

    /** Trims and collapses whitespace runs, the same cleanup the server applies to `q`. */
    fun cleanQuery(text: String): String = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

    /** URL query string. URLEncoder turns "+" into %2B and spaces into "+", which the server reads back as spaces. */
    fun buildQuery(params: Map<String, String>): String =
        params.entries.joinToString("&") { (k, v) -> "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }

    fun fetchHistory(): SearchHistoryResult = try {
        val conn = open("/search", "GET", mapOf("include" to "history"))
        val code = codeOf(conn)
        val body = bodyOf(conn)
        conn.disconnect()
        when (code) {
            200 -> parseHistory(body)
            401 -> SearchHistoryResult(SearchStatus.AUTH_ERROR)
            else -> {
                println("❌ [SearchApi] load history returned status $code")
                SearchHistoryResult(SearchStatus.NETWORK_ERROR)
            }
        }
    } catch (e: Exception) {
        println("❌ [SearchApi] load history error: ${e.message}")
        SearchHistoryResult(SearchStatus.NETWORK_ERROR)
    }

    /** Brand typeahead for an already-cleaned query. Null on any error. */
    fun fetchBrands(query: String): BrandSearchResult? = try {
        val conn = open("/search", "GET", mapOf("include" to "brands", "q" to query))
        val code = codeOf(conn)
        val body = bodyOf(conn)
        conn.disconnect()
        if (code == 200) parseBrands(body) else {
            println("❌ [SearchApi] brand search returned status $code")
            null
        }
    } catch (e: Exception) {
        println("❌ [SearchApi] brand search error: ${e.message}")
        null
    }

    /** Records a submitted search and/or a visited brand in one request. Fire-and-forget. */
    fun recordHistory(query: String?, brandId: String?) {
        val body = JSONObject()
        if (query != null) body.put("query", query)
        if (brandId != null) body.put("brandId", brandId)
        if (body.length() == 0) return
        try {
            val conn = open("/search/history", "POST")
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = codeOf(conn)
            if (code != 200) println("❌ [SearchApi] save history returned status $code: ${bodyOf(conn)}")
            conn.disconnect()
        } catch (e: Exception) {
            println("❌ [SearchApi] failed to save history: ${e.message}")
        }
    }

    /** Clears the user's recent searches (not visited stores). */
    fun clearSearches(): Boolean = try {
        val conn = open("/search/history", "DELETE", mapOf("type" to "searches"))
        val code = codeOf(conn)
        conn.disconnect()
        if (code != 200) println("⚠️ [SearchApi] Clear All returned status $code")
        code == 200
    } catch (e: Exception) {
        println("❌ [SearchApi] Clear All failed: ${e.message}")
        false
    }

    /** Brand types that open the grocery store screen (aisles), same as the Home grocery grid. */
    private val groceryBrandTypes = setOf("grocery")

    fun isGroceryType(brandType: String): Boolean = brandType.lowercase() in groceryBrandTypes

    // MARK: - Parsing (pure, unit-tested). Missing or null fields fall back to defaults,
    // and a malformed item is skipped instead of failing the whole list.

    fun parseHistory(json: String): SearchHistoryResult {
        val obj = JSONObject(json)
        val searches = obj.optJSONArray("recentSearches").objects().mapNotNull { item ->
            val query = item.str("query")
            if (query.isEmpty()) null else RecentSearchEntry(query, item.optInt("count", 0))
        }
        val visited = obj.optJSONArray("recentlyVisitedBrands").objects().mapNotNull { item ->
            val id = item.str("brandId")
            if (id.isEmpty()) null else VisitedBrand(
                brandId = id,
                brandName = item.str("brandName"),
                logoUrl = item.str("logoUrl"),
                tags = item.optJSONArray("tags").strings(),
                visitedAt = item.str("visitedAt").ifEmpty { null },
                brandType = item.str("brandType")
            )
        }
        return SearchHistoryResult(SearchStatus.SUCCESS, searches, visited)
    }

    fun parseBrands(json: String): BrandSearchResult {
        val obj = JSONObject(json)
        val brands = obj.optJSONArray("brands").objects().mapNotNull { item ->
            val id = item.str("id")
            if (id.isEmpty()) null else BrandSuggestion(
                id = id,
                name = item.str("name"),
                logoUrl = item.str("logoUrl"),
                tags = item.optJSONArray("tags").strings(),
                brandType = item.str("brandType")
            )
        }
        return BrandSearchResult(obj.str("query"), brands)
    }

    /** optString returns the literal "null" for JSON null; this returns "" instead. */
    private fun JSONObject.str(key: String): String =
        if (isNull(key)) "" else optString(key, "")

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { i -> if (isNull(i)) null else optString(i) }

    // MARK: - HTTP

    private fun open(path: String, method: String, params: Map<String, String> = emptyMap()): HttpURLConnection {
        if (AuthManager.getToken().isNullOrBlank()) HomeFDData.ensureToken()
        val query = if (params.isEmpty()) "" else "?" + buildQuery(params)
        val conn = URL("$API_BASE_URL$path$query").openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("Content-Type", "application/json")
        AuthManager.getToken()?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
        return conn
    }

    private fun codeOf(conn: HttpURLConnection): Int = try { conn.responseCode } catch (_: Exception) { -1 }

    private fun bodyOf(conn: HttpURLConnection): String = try {
        if (codeOf(conn) in 200..299) conn.inputStream.bufferedReader().use { it.readText() }
        else conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
    } catch (_: Exception) {
        ""
    }
}
