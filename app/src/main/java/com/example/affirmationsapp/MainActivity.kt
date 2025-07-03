package com.example.affirmationsapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import org.json.JSONObject
import android.widget.Toast
import android.content.Intent


class MainActivity : AppCompatActivity() {

    private val PREFS_NAME = "CindyPrefs"
    private val KEY_REMAINING = "remaining_affirmations"
    private val KEY_HISTORY = "history"

    private fun saveRemainingAffirmations(context: Context, data: Map<String, List<String>>) {
        val json = JSONObject(data.mapValues { it.value }).toString()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REMAINING, json)
            .apply()
    }

    private fun loadRemainingAffirmations(context: Context, original: Map<String, List<String>>): Map<String, MutableList<String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_REMAINING, null)

        return if (jsonString != null) {
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
            result
        } else {
            // No saved data? Start fresh
            original.mapValues { it.value.toMutableList() }.toMutableMap()
        }
    }

    private fun saveToHistory(context: Context, category: String, affirmation: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyJson = prefs.getString(KEY_HISTORY, "[]")
        val historyArray = org.json.JSONArray(historyJson)

        val entry = JSONObject()
        entry.put("timestamp", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date()))
        entry.put("category", category)
        entry.put("text", affirmation)

        historyArray.put(entry)

        prefs.edit().putString(KEY_HISTORY, historyArray.toString()).apply()
    }

    private fun loadHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyJson = prefs.getString(KEY_HISTORY, "[]")
        val historyArray = org.json.JSONArray(historyJson)
        val result = mutableListOf<String>()
        for (i in 0 until historyArray.length()) {
            val obj = historyArray.getJSONObject(i)
            val text = "[${obj.getString("timestamp")}] ${obj.getString("category")}: ${obj.getString("text")}"
            result.add(text)
        }
        return result
    }

    private fun loadCategoryMapFromJson(context: Context): Map<String, List<String>> {
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
        return result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.textMessage)

        val categoryMap = loadCategoryMapFromJson(this)

        findViewById<Button>(R.id.buttonYou).setOnClickListener {
            showRandomFromCategory("You", categoryMap, textView)
        }

        findViewById<Button>(R.id.buttonReflections).setOnClickListener {
            showRandomFromCategory("Reflections", categoryMap, textView)
        }

        findViewById<Button>(R.id.buttonAspirations).setOnClickListener {
            showRandomFromCategory("Aspirations", categoryMap, textView)
        }

        findViewById<Button>(R.id.buttonHorny).setOnClickListener {
            showRandomFromCategory("The Hornery Button!", categoryMap, textView)
        }

        findViewById<Button>(R.id.buttonMystery).setOnClickListener {
            showRandomFromCategory("???", categoryMap, textView)
        }

        findViewById<Button>(R.id.buttonViewHistory).setOnClickListener {
            val intent = android.content.Intent(this, HistoryActivity::class.java)
            startActivity(intent)

        }
        val clearBtn = findViewById<Button>(R.id.buttonClearHistory)
        clearBtn.setOnClickListener {
            val prefs = getSharedPreferences("CindyPrefs", Context.MODE_PRIVATE)
            prefs.edit().remove("history").apply()
            textView.text = "History cleared!"
        }
        findViewById<Button>(R.id.buttonReset).setOnClickListener {
            val original = loadCategoryMapFromJson(this)
            saveRemainingAffirmations(this, original)
        }
        findViewById<Button>(R.id.buttonAbout).setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showRandomFromCategory(
        category: String,
        originalCategoryMap: Map<String, List<String>>,
        textView: TextView
    ) {
        val remainingMap = loadRemainingAffirmations(this, originalCategoryMap).toMutableMap()
        val list = remainingMap[category]

        if (list.isNullOrEmpty()) {
            Toast.makeText(this, "No more affirmations in $category!", Toast.LENGTH_SHORT).show()
        } else {
            val random = list.random()
            textView.text = random

            // Remove the used affirmation
            remainingMap[category]?.remove(random)

            // Save updated affirmations
            saveRemainingAffirmations(this, remainingMap)

            // Save to history
            saveToHistory(this, category, random)
        }
    }
}
