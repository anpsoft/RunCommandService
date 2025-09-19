package com.yourcompany.yourapp

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.widget.RemoteViews
import android.content.ComponentName
import android.os.Environment
import android.graphics.BitmapFactory
import java.io.File

class ScriptWidget : AppWidgetProvider() {
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            val scriptData = getWidgetData(context, widgetId)
            if (scriptData.isNotEmpty()) {
                updateWidget(context, appWidgetManager, widgetId, scriptData)
            }
        }
    }
    
    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, data: Map<String, String>) {
        val intent = Intent(context, ShortcutActivity::class.java).apply {
            action = "RUN_SCRIPT"
            putExtra("script_path", data["path"])
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, widgetId, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val views = RemoteViews(context.packageName, android.R.layout.activity_list_item).apply {
            setTextViewText(android.R.id.text1, data["name"])
            
            // Иконка
            val iconPath = data["icon"]
            if (!iconPath.isNullOrEmpty()) {
                val iconFile = File(Environment.getExternalStorageDirectory(), "MyScripts/icons/$iconPath")
                if (iconFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(iconFile.absolutePath)
                    setImageViewBitmap(android.R.id.icon, bitmap)
                } else {
                    setImageViewResource(android.R.id.icon, getIconResource(iconPath))
                }
            } else {
                setImageViewResource(android.R.id.icon, R.mipmap.ic_launcher)
            }
            
            setOnClickPendingIntent(android.R.id.text1, pendingIntent)
        }
        
        appWidgetManager.updateAppWidget(widgetId, views)
    }
    
    private fun getWidgetData(context: Context, widgetId: Int): Map<String, String> {
        val prefs = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)
        val path = prefs.getString("path_$widgetId", "") ?: ""
        val name = prefs.getString("name_$widgetId", "") ?: ""
        val icon = prefs.getString("icon_$widgetId", "") ?: ""
        return if (path.isNotEmpty()) mapOf("path" to path, "name" to name, "icon" to icon) else emptyMap()
    }
    
    private fun getIconResource(iconName: String): Int {
        return when (iconName) {
            "icon.png" -> R.mipmap.ic_launcher
            "Terminal.png" -> R.mipmap.ic_shortcut
            "no_icon.png" -> R.mipmap.ic_no_icon
            else -> R.mipmap.ic_no_icon
        }
    }
}