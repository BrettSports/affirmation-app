package com.example.affirmationsapp

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val textView = findViewById<TextView>(R.id.historyText)
        val prefs = getSharedPreferences("CindyPrefs", Context.MODE_PRIVATE)
        val historyJson = prefs.getString("history", "[]")
        val historyArray = JSONArray(historyJson)

        val result = StringBuilder()
        for (i in 0 until historyArray.length()) {
            val obj = historyArray.getJSONObject(i)
            val line = "[${obj.getString("timestamp")}] ${obj.getString("category")}: ${obj.getString("text")}"
            result.append(line).append("\n\n")
        }

        textView.text = result.toString().ifBlank { "No history yet!" }
    }
}
