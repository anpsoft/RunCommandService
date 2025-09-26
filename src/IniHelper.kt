package com.yourcompany.yourapp

import android.content.Context
import android.content.SharedPreferences
import org.ini4j.Ini
import java.io.File

object IniHelper {
    private lateinit var iniFile: File
    private val ini = Ini()

fun init(context: Context) {
    // Сначала определяем путь БЕЗ записи в ini
    val scriptsDir = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .getString("scripts_dir", "/sdcard/MyScripts") ?: "/sdcard/MyScripts"
    
    iniFile = File(scriptsDir, "scripts.ini")
    iniFile.parentFile?.mkdirs()
    
    // Загружаем существующий ini если есть
    if (iniFile.exists()) {
        try {
            ini.load(iniFile)
        } catch (e: Exception) {
            // Файл поврежден - будем пересоздавать
        }
    }
    
    // Теперь синхронизируем настройки (с записью)
    syncSettingsWithPrefs(context)
}

private fun syncSettingsWithPrefs(context: Context) {
    // Читаем без вызова save() внутри
    val scriptsDir = readScriptsDir(context)
    val iconsDir = readIconsDir(context)
    
    // Записываем в ini только если что-то изменилось
    val settingsSection = ini.add("settings")
    var changed = false
    
    if (settingsSection["scripts_dir"] != scriptsDir) {
        settingsSection["scripts_dir"] = scriptsDir
        changed = true
    }
    
    if (settingsSection["icons_dir"] != iconsDir) {
        settingsSection["icons_dir"] = iconsDir
        changed = true
    }
    
    if (changed) {
        save()
    }
}


    private fun getSettingsFile(context: Context): File {
        val scriptsDir = getScriptsDir(context)
        return File(scriptsDir, "scripts.ini")
    }

    fun getScriptsDir(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val iniValue = ini["settings"]?.get("scripts_dir") ?: "/sdcard/MyScripts"
        if (iniValue != "/sdcard/MyScripts" && iniValue.isNotEmpty()) {
            prefs.edit().putString("scripts_dir", iniValue).apply()
            return iniValue
        }
        val prefValue = prefs.getString("scripts_dir", "/sdcard/MyScripts") ?: "/sdcard/MyScripts"
        ini.add("settings")["scripts_dir"] = prefValue
        save()
        return prefValue
    }

    fun getIconsDir(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val iniValue = ini["settings"]?.get("icons_dir") ?: "/sdcard/MyScripts/icons"
        if (iniValue != "/sdcard/MyScripts/icons" && iniValue.isNotEmpty()) {
            prefs.edit().putString("icons_dir", iniValue).apply()
            return iniValue
        }
        val prefValue = prefs.getString("icons_dir", "/sdcard/MyScripts/icons") ?: "/sdcard/MyScripts/icons"
        ini.add("settings")["icons_dir"] = prefValue
        save()
        return prefValue
    }

    fun updateSettings(context: Context, scriptsDir: String, iconsDir: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("scripts_dir", scriptsDir).putString("icons_dir", iconsDir).apply()
        val section = ini.add("settings")
        section["scripts_dir"] = scriptsDir
        section["icons_dir"] = iconsDir
        save()
    }



fun getScriptConfig(scriptName: String): ScriptConfig {
    return try {
        val section = ini[scriptName]
        if (section != null) {
            ScriptConfig(
                name = section.get("name", ""),
                description = section.get("description", ""),
                icon = section.get("icon", ""),
                isActive = section.get("is_active", "true").toBoolean(),
                hasShortcut = section.get("has_shortcut", "false").toBoolean()
            )
        } else {
            // Для новых скриптов - активны по умолчанию
            ScriptConfig(isActive = true)
        }
    } catch (e: Exception) {
        ScriptConfig(isActive = true)
    }
}

    fun updateScriptConfig(scriptName: String, config: ScriptConfig) {
        val section = ini[scriptName] ?: ini.add(scriptName)
        section["name"] = config.name
        section["description"] = config.description
        section["icon"] = config.icon
        section["is_active"] = config.isActive.toString()
        section["has_shortcut"] = config.hasShortcut.toString()
        save()
    }

    fun addScriptConfig(scriptName: String, config: ScriptConfig) {
        updateScriptConfig(scriptName, config)
    }

    fun renameScriptConfig(oldName: String, newName: String, config: ScriptConfig) {
        ini.remove(oldName)
        updateScriptConfig(newName, config)
    }

    fun deleteScriptConfig(scriptName: String) {
        ini.remove(scriptName)
        save()
    }

    fun cleanupOrphanedConfigs(context: Context) {
        val scriptsDir = File(getScriptsDir(context))
        val existingFiles = scriptsDir.listFiles { _, name -> name.endsWith(".sh") }
            ?.map { it.nameWithoutExtension }?.toSet() ?: emptySet()
        
        val sectionsToRemove = mutableListOf<String>()
        for (sectionName in ini.keys) {
            if (!existingFiles.contains(sectionName) && sectionName != "settings") {
                sectionsToRemove.add(sectionName)
            }
        }
        
        sectionsToRemove.forEach { ini.remove(it) }
        if (sectionsToRemove.isNotEmpty()) {
            save()
        }
    }

fun createShortcutsForExisting(context: Context) {
        for (sectionName in ini.keys) {
            if (sectionName == "settings") continue
            val config = getScriptConfig(sectionName)
            if (config.hasShortcut) {
                val scriptPath = "${getScriptsDir(context)}/$sectionName.sh"
                ShortcutManager.createShortcut(context, sectionName, scriptPath, config.icon)
            }
        }
    }

    private fun save() {
        ini.store(iniFile)
    }
}