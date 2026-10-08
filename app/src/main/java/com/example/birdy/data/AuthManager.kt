package com.example.birdy.data

import android.content.Context
import com.birdy.kit.chat.OrderChatSocket
import android.content.SharedPreferences

/**
 * AuthManager — mirrors iOS AuthManager.swift
 * Stores/retrieves JWT token using SharedPreferences (Android equivalent of iOS Keychain).
 * Singleton pattern, initialized once with application context.
 */
object AuthManager {
    private const val PREFS_NAME = "com.birdyone.auth"
    private const val KEY_AUTH_TOKEN = "authToken"
    private const val KEY_USER_FIRST_NAME = "userFirstName"
    private const val KEY_USER_LAST_NAME = "userLastName"
    private const val KEY_USER_EMAIL = "userEmail"
    private const val KEY_USER_ID = "userID"

    private lateinit var prefs: SharedPreferences

    /** Call once in Application.onCreate() or from any Context early on. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Lazy init fallback — uses the given context if not yet initialized. */
    private fun ensurePrefs(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // ── Token ──────────────────────────────────────────────

    fun setToken(token: String?, context: Context? = null) {
        if (context != null) ensurePrefs(context)
        if (!::prefs.isInitialized) return
        if (token != null) {
            prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
            // Grocery lines added while signed out join the saved cart.
            CartManager.signedIn()
        } else {
            clearToken()
        }
    }

    fun getToken(context: Context? = null): String? {
        if (context != null) ensurePrefs(context)
        if (!::prefs.isInitialized) return null
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun clearToken() {
        if (!::prefs.isInitialized) return
        prefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }

    fun isLoggedIn(context: Context? = null): Boolean {
        return !getToken(context).isNullOrEmpty()
    }

    // ── User Profile (mirrors iOS @Published vars) ─────────

    fun setUserFirstName(name: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY_USER_FIRST_NAME, name).apply()
    }

    fun getUserFirstName(): String {
        if (!::prefs.isInitialized) return ""
        return prefs.getString(KEY_USER_FIRST_NAME, "") ?: ""
    }

    fun setUserLastName(name: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY_USER_LAST_NAME, name).apply()
    }

    fun getUserLastName(): String {
        if (!::prefs.isInitialized) return ""
        return prefs.getString(KEY_USER_LAST_NAME, "") ?: ""
    }

    fun setUserEmail(email: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String {
        if (!::prefs.isInitialized) return ""
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun setUserID(id: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY_USER_ID, id).apply()
        // Sign-in sets the token first, then the id: now the cart topic can be joined.
        if (id.isNotEmpty()) CartManager.startLiveUpdates()
    }

    fun getUserID(): String {
        if (!::prefs.isInitialized) return ""
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    // ── Profile Image URL ─────────────────────────────────
    private const val KEY_PROFILE_IMAGE_URL = "profileImageUrl"

    fun setProfileImageUrl(url: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY_PROFILE_IMAGE_URL, url).apply()
    }

    fun getProfileImageUrl(): String {
        if (!::prefs.isInitialized) return ""
        return prefs.getString(KEY_PROFILE_IMAGE_URL, "") ?: ""
    }

    // ── User Rating ───────────────────────────────────────
    private const val KEY_USER_RATING = "userRating"

    fun setUserRating(rating: Float) {
        if (!::prefs.isInitialized) return
        prefs.edit().putFloat(KEY_USER_RATING, rating).apply()
    }

    fun getUserRating(): Float {
        if (!::prefs.isInitialized) return 5.0f
        return prefs.getFloat(KEY_USER_RATING, 5.0f)
    }

    /** Clear everything on sign-out / account deletion. */
    fun clearAll() {
        // Stop the live order chat stream for this (now signed-out) user.
        OrderChatSocket.disconnect()
        // The saved cart belongs to the account; the phone's cart stays.
        CartManager.signedOut()
        if (!::prefs.isInitialized) return
        prefs.edit().clear().apply()
    }
}