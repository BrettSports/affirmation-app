package com.example.affirmationsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val textView = findViewById<TextView>(R.id.textAbout)
        val aboutText = assets.open("about.txt").bufferedReader().use { it.readText() }
        textView.text = aboutText
    }
}