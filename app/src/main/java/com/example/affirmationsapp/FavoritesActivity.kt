package com.example.affirmationsapp

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import android.util.Log

class FavoritesActivity : AppCompatActivity() {

    private val PREFS_NAME = "CindyPrefs"
    private val KEY_FAVORITES = "favorites"
    private val TAG = "FavoritesActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        val textView = findViewById<TextView>(R.id.textFavorites)
        val clearButton = findViewById<Button>(R.id.buttonClearFavorites)

        val favoritesList = loadFavorites()

        textView.text = if (favoritesList.isEmpty()) {
            "No favorites yet! Tap ⭐ to save affirmations."
        } else {
            favoritesList.joinToString(separator = "\n\n") { "⭐ $it" }
        }

        clearButton.setOnClickListener {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_FAVORITES).apply()
            textView.text = "Favorites cleared!"
        }
    }

    private fun loadFavorites(): List<String> {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val favoritesList = mutableListOf<String>()

        try {
            // First, try to read as JSON string (new format)
            val favoritesJson = prefs.getString(KEY_FAVORITES, null)
            if (favoritesJson != null) {
                Log.d(TAG, "Found favorites as JSON string")
                val favoritesArray = JSONArray(favoritesJson)
                for (i in 0 until favoritesArray.length()) {
                    favoritesList.add(favoritesArray.getString(i))
                }
                return favoritesList
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to read as JSON, trying StringSet format")
            // If JSON parsing fails, try the old StringSet format
            try {
                val favoritesSet = prefs.getStringSet(KEY_FAVORITES, null)
                if (favoritesSet != null) {
                    Log.d(TAG, "Found favorites as StringSet, converting to JSON")
                    favoritesList.addAll(favoritesSet)

                    // Convert to new JSON format for future use
                    convertToJsonFormat(favoritesList)
                    return favoritesList
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to read favorites in any format", e2)
            }
        }

        // If all else fails, return empty list
        Log.d(TAG, "No favorites found or failed to read")
        return favoritesList
    }

    private fun convertToJsonFormat(favoritesList: List<String>) {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            for (favorite in favoritesList) {
                jsonArray.put(favorite)
            }

            // Save in new JSON format and remove old StringSet
            prefs.edit()
                .putString(KEY_FAVORITES, jsonArray.toString())
                .remove(KEY_FAVORITES) // This removes the StringSet
                .apply()

            // Re-save as JSON
            prefs.edit()
                .putString(KEY_FAVORITES, jsonArray.toString())
                .apply()

            Log.d(TAG, "Converted favorites from StringSet to JSON format")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert favorites to JSON format", e)
        }
    }
}