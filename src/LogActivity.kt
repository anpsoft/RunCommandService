package com.yourcompany.yourapp5

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.ClipData
import android.content.ClipboardManager

class LogActivity : Activity() {
    private lateinit var textView: TextView
    private var logsText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        textView = TextView(this).apply {
            setPadding(16, 16, 16, 16)
            textSize = 14f
            setTextIsSelectable(true)
        }

        val scrollView = ScrollView(this).apply {
            addView(textView)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        layout.addView(scrollView)

        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val clearButton = Button(this).apply {
            text = "Очистить лог"
            setOnClickListener {
                clearLogs()
                loadLogs()
            }
        }

        val copyButton = Button(this).apply {
            text = "Копировать"
            setOnClickListener {
                copyToClipboard()
            }
        }

        buttonsLayout.addView(clearButton)
        buttonsLayout.addView(copyButton)
        layout.addView(buttonsLayout)

        setContentView(layout)
        loadLogs()
    }

    private fun loadLogs() {
        try {
            val process = Runtime.getRuntime().exec("logcat -d")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val logs = StringBuilder()
            reader.useLines { lines -> lines.forEach { logs.append(it).append("\n") } }
            logsText = if (logs.isNotEmpty()) logs.toString() else "Логи отсутствуют"
            textView.text = logsText
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка чтения логов: ${e.message}", Toast.LENGTH_LONG).show()
            textView.text = "Не удалось загрузить логи"
        }
    }

    private fun clearLogs() {
        try {
            Runtime.getRuntime().exec("logcat -c")
            Toast.makeText(this, "Логи очищены", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка очистки логов: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard() {
        if (logsText.isNotEmpty()) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Логи", logsText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Логи скопированы", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Нет логов для копирования", Toast.LENGTH_SHORT).show()
        }
    }
}