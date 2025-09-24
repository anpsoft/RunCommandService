package com.yourcompany.yourapp

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader

class InstructionsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
            textSize = 14f
        }

        try {
            val input = assets.open("instructions.txt")
            val reader = BufferedReader(InputStreamReader(input))
            textView.text = reader.readText()
            reader.close()
        } catch (e: Exception) {
            textView.text = "Файл не найден"
        }

        scrollView.addView(textView)
        setContentView(scrollView)
    }

    private val Int.dp: Int = (this * resources.displayMetrics.density).toInt()
}