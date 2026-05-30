package com.example.birdy.data

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Checks the backend /app-version endpoint and compares ac_min_version
 * against the app's current version name. If current < minimum, triggers force update.
 */
object ForceUpdateChecker {

    private const val TAG = "ForceUpdate"

    data class Result(
        val needsUpdate: Boolean,
        val minVersion: String = "0.0.0"
    )

    suspend fun check(context: Context): Result = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion(context)
            val url = URL("${Config.API_BASE_URL}/app-version")
            val json = url.readText()
            val obj = JSONObject(json)
            val acMinVersion = obj.optString("ac_min_version", "0.0.0")

            val needsUpdate = compareVersions(currentVersion, acMinVersion)
            Log.d(TAG, "Version check: current=$currentVersion, min=$acMinVersion, needsUpdate=$needsUpdate")

            Result(needsUpdate = needsUpdate, minVersion = acMinVersion)
        } catch (e: Exception) {
            Log.e(TAG, "Force update check failed: ${e.message}")
            Result(needsUpdate = false)
        }
    }

    private fun getCurrentVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    /**
     * Returns true if current < minimum (i.e. app needs update)
     */
    private fun compareVersions(current: String, minimum: String): Boolean {
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        val m = minimum.split(".").mapNotNull { it.toIntOrNull() }
        val pc = pad(c)
        val pm = pad(m)
        for (i in 0 until 3) {
            if (pc[i] < pm[i]) return true
            if (pc[i] > pm[i]) return false
        }
        return false
    }

    private fun pad(parts: List<Int>): List<Int> {
        val result = parts.toMutableList()
        while (result.size < 3) result.add(0)
        return result.take(3)
    }

    fun openPlayStore(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${context.packageName}".toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}