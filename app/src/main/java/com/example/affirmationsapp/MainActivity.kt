package com.example.affirmationsapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import org.json.JSONObject
import org.json.JSONArray
import android.widget.Toast
import android.content.Intent
import android.util.Log


class MainActivity : AppCompatActivity() {

    private val PREFS_NAME = "CindyPrefs"
    private val KEY_REMAINING = "remaining_affirmations"
    private val KEY_HISTORY = "history"
    private val KEY_FAVORITES = "favorites"
    private val TAG = "MainActivity"

    private var lastShownAffirmation: String? = null
    private lateinit var favoriteButton: ImageButton

    private lateinit var categoryMap: Map<String, List<String>>
    private lateinit var remainingMap: MutableMap<String, MutableList<String>>

    private fun saveRemainingAffirmations(context: Context, data: Map<String, List<String>>) {
        try {
            val json = JSONObject(data.mapValues { it.value }).toString()
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_REMAINING, json)
                .apply()
            Log.d(TAG, "Saved remaining affirmations successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving remaining affirmations", e)
        }
    }

    private fun loadRemainingAffirmations(context: Context, original: Map<String, List<String>>): MutableMap<String, MutableList<String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_REMAINING, null)

        return if (jsonString != null) {
            try {
                val jsonObject = JSONObject(jsonString)
                val result = mutableMapOf<String, MutableList<String>>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val category = keys.next()
                    val jsonArray = jsonObject.getJSONArray(category)
                    val list = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        list.add(jsonArray.getString(i))
                    }
                    result[category] = list
                }
                Log.d(TAG, "Loaded remaining affirmations from prefs")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error loading remaining affirmations, starting fresh", e)
                original.mapValues { it.value.toMutableList() }.toMutableMap()
            }
        } else {
            Log.d(TAG, "No saved data, starting fresh")
            original.mapValues { it.value.toMutableList() }.toMutableMap()
        }
    }

    private fun saveToHistory(context: Context, category: String, affirmation: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val historyJson = prefs.getString(KEY_HISTORY, "[]")
            val historyArray = JSONArray(historyJson)

            val entry = JSONObject()
            entry.put("timestamp", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date()))
            entry.put("category", category)
            entry.put("text", affirmation)

            historyArray.put(entry)

            prefs.edit().putString(KEY_HISTORY, historyArray.toString()).apply()
            Log.d(TAG, "Saved to history: $category - $affirmation")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to history", e)
        }
    }

    private fun loadCategoryMapFromJson(context: Context): Map<String, List<String>> {
        try {
            val jsonString = context.assets.open("affirmations.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            val result = mutableMapOf<String, List<String>>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val category = keys.next()
                val jsonArray = jsonObject.getJSONArray(category)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    list.add(jsonArray.getString(i))
                }
                result[category] = list
            }
            Log.d(TAG, "Loaded categories: ${result.keys}")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error loading affirmations.json", e)
            Toast.makeText(context, "Error loading affirmations file", Toast.LENGTH_LONG).show()
            return emptyMap()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "onCreate started")

        try {
            val textView = findViewById<TextView>(R.id.textAffirmation)
            textView.movementMethod = android.text.method.ScrollingMovementMethod()
            textView.setOnTouchListener { v, event ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and android.view.MotionEvent.ACTION_MASK) {
                    android.view.MotionEvent.ACTION_UP -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }
            favoriteButton = findViewById(R.id.buttonFavorite)

            // Load data first
            categoryMap = loadCategoryMapFromJson(this)
            if (categoryMap.isEmpty()) {
                Log.e(TAG, "Category map is empty!")
                Toast.makeText(this, "Failed to load affirmations", Toast.LENGTH_LONG).show()
                return
            }

            remainingMap = loadRemainingAffirmations(this, categoryMap)
            Log.d(TAG, "Remaining map keys: ${remainingMap.keys}")

            // Set up buttons
            setupButtons(textView)

            Log.d(TAG, "onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupButtons(textView: TextView) {
        try {
            findViewById<Button>(R.id.buttonViewFavorites).setOnClickListener {
                val intent = Intent(this, FavoritesActivity::class.java)
                startActivity(intent)
            }

            favoriteButton.setOnClickListener {
                lastShownAffirmation?.let { affirmation ->
                    if (isFavorite(this, affirmation)) {
                        Toast.makeText(this, "Already in favorites!", Toast.LENGTH_SHORT).show()
                    } else {
                        saveToFavorites(this, affirmation)
                        favoriteButton.setImageResource(R.drawable.ic_star_filled)
                        Toast.makeText(this, "Saved to favorites!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            findViewById<Button>(R.id.buttonYou).setOnClickListener {
                Log.d(TAG, "You button clicked")
                showRandomFromCategory("You", textView)
            }

            findViewById<Button>(R.id.buttonReflections).setOnClickListener {
                Log.d(TAG, "Reflections button clicked")
                showRandomFromCategory("Reflections", textView)
            }

            findViewById<Button>(R.id.buttonAspirations).setOnClickListener {
                Log.d(TAG, "Aspirations button clicked")
                showRandomFromCategory("Aspirations", textView)
            }

            findViewById<Button>(R.id.buttonHorny).setOnClickListener {
                Log.d(TAG, "Horny button clicked")
                showRandomFromCategory("The Hornery Button!", textView)
            }

            findViewById<Button>(R.id.buttonMystery).setOnClickListener {
                Log.d(TAG, "Mystery button clicked")
                showRandomFromCategory("???", textView)
            }

            findViewById<Button>(R.id.buttonViewHistory).setOnClickListener {
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
            }

            findViewById<Button>(R.id.buttonClearHistory).setOnClickListener {
                val prefs = getSharedPreferences("CindyPrefs", Context.MODE_PRIVATE)
                prefs.edit().remove("history").apply()
                textView.text = "History cleared!"
            }

            findViewById<Button>(R.id.buttonReset).setOnClickListener {
                val original = loadCategoryMapFromJson(this)
                remainingMap = original.mapValues { it.value.toMutableList() }.toMutableMap()
                saveRemainingAffirmations(this, remainingMap)
                textView.text = "Affirmations reset!"
                textView.scrollTo(0, 0)
            }

            findViewById<Button>(R.id.buttonAbout).setOnClickListener {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up buttons", e)
            Toast.makeText(this, "Error setting up buttons: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showRandomFromCategory(category: String, textView: TextView) {
        try {
            Log.d(TAG, "showRandomFromCategory called with: $category")

            val list = remainingMap[category]
            Log.d(TAG, "List for $category: ${list?.size} items")

            if (list.isNullOrEmpty()) {
                Log.d(TAG, "No more affirmations in $category")
                Toast.makeText(this, "No more affirmations in $category!", Toast.LENGTH_SHORT).show()
            } else {
                val random = list.random()
                Log.d(TAG, "Selected random affirmation: $random")

                textView.text = random
                textView.scrollTo(0, 0)
                lastShownAffirmation = random

                // Update favorite button state
                if (isFavorite(this, random)) {
                    favoriteButton.setImageResource(R.drawable.ic_star_filled)
                } else {
                    favoriteButton.setImageResource(R.drawable.ic_star_border)
                }

                // Remove the used affirmation
                list.remove(random)
                Log.d(TAG, "Removed affirmation, remaining: ${list.size}")

                // Save updated affirmations
                saveRemainingAffirmations(this, remainingMap)

                // Save to history
                saveToHistory(this, category, random)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in showRandomFromCategory", e)
            Toast.makeText(this, "Error showing affirmation: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveToFavorites(context: Context, affirmation: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val favoritesJson = prefs.getString(KEY_FAVORITES, "[]")
            val favoritesArray = JSONArray(favoritesJson)

            // Check if already exists
            var alreadyExists = false
            for (i in 0 until favoritesArray.length()) {
                if (favoritesArray.getString(i) == affirmation) {
                    alreadyExists = true
                    break
                }
            }

            if (!alreadyExists) {
                favoritesArray.put(affirmation)
                prefs.edit().putString(KEY_FAVORITES, favoritesArray.toString()).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to favorites", e)
        }
    }

    private fun isFavorite(context: Context, affirmation: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val favoritesJson = prefs.getString(KEY_FAVORITES, "[]")
            val favoritesArray = JSONArray(favoritesJson)

            for (i in 0 until favoritesArray.length()) {
                if (favoritesArray.getString(i) == affirmation) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if favorite", e)
            false
        }
    }
}