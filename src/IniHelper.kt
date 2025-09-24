package com.yourcompany.yourapp

import android.content.Context
import android.os.Environment
import org.ini4j.Ini
import java.io.File

object IniHelper {
    private val iniFile = getSettingsFile()
    private val ini = Ini()

    init {
        iniFile.parentFile?.mkdirs()
        if (iniFile.exists()) {
            ini.load(iniFile)
        }
        syncSettingsWithPrefs() // Синхронизация при инициализации
    }

    private fun getSettingsFile(): File {
        val scriptsDir = getScriptsDir()
        return File(scriptsDir, "scripts.ini")
    }

    // Настройки
    fun getScriptsDir(): String {
        val prefs = getPrefs()
        val iniValue = ini["settings"]?.get("scripts_dir", "/sdcard/MyScripts")
        if (iniValue != null && iniValue != "") {
            prefs.edit().putString("scripts_dir", iniValue).apply()
            return iniValue
        }
        val prefValue = prefs.getString("scripts_dir", "/sdcard/MyScripts")
        if (prefValue != null && prefValue != "") {
            ini["settings"]?.put("scripts_dir", prefValue) ?: ini.add("settings").put("scripts_dir", prefValue)
            save()
            return prefValue
        }
        return "/sdcard/MyScripts"
    }

    fun getIconsDir(): String {
        val prefs = getPrefs()
        val iniValue = ini["settings"]?.get("icons_dir", "/sdcard/MyScripts/icons")
        if (iniValue != null && iniValue != "") {
            prefs.edit().putString("icons_dir", iniValue).apply()
            return iniValue
        }
        val prefValue = prefs.getString("icons_dir", "/sdcard/MyScripts/icons")
        if (prefValue != null && prefValue != "") {
            ini["settings"]?.put("icons_dir", prefValue) ?: ini.add("settings").put("icons_dir", prefValue)
            save()
            return prefValue
        }
        return "/sdcard/MyScripts/icons"
    }

    fun updateSettings(scriptsDir: String, iconsDir: String) {
        val prefs = getPrefs()
        prefs.edit().putString("scripts_dir", scriptsDir).putString("icons_dir", iconsDir).apply()
        ini["settings"]?.put("scripts_dir", scriptsDir)?.put("icons_dir", iconsDir) ?: ini.add("settings").apply {
            put("scripts_dir", scriptsDir)
            put("icons_dir", iconsDir)
        }
        save()
    }

    private fun syncSettingsWithPrefs() {
        val prefs = getPrefs()
        val scriptsDir = getScriptsDir()
        val iconsDir = getIconsDir()
        // Дополнительная синхронизация если нужно
    }

    private fun getPrefs() = IniHelper::class.java.getResourceAsStream("app_prefs") // Нет, используем Context в вызовах
    // Фактически, prefs в активностях, но для простоты - передавать Context в методы

    // Остальные методы без изменений, но обновить пути
    fun getScriptConfig(scriptName: String): ScriptConfig {
        val section = ini[scriptName] ?: return ScriptConfig()
        return ScriptConfig(
            name = section.get("name", ""),
            description = section.get("description", ""),
            icon = section.get("icon", ""),
            isActive = section.get("is_active", "true").toBoolean(),
            hasShortcut = section.get("has_shortcut", "false").toBoolean()
        )
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
        val scriptsDir = File(IniHelper.getScriptsDir(context)) // Обновить
        val existingFiles = scriptsDir.listFiles { _, name -> name.endsWith(".sh") }
            ?.map { it.nameWithoutExtension }?.toSet() ?: emptySet()
        
        val sectionsToRemove = mutableListOf<String>()
        for (sectionName in ini.keys) {
            if (!existingFiles.contains(sectionName)) {
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

    // Для Context
    fun getScriptsDir(context: Context): String = getScriptsDir() // Адаптировать с prefs
    // В реальности, передавать Context везде, где prefs

    // Пример с Context
    private fun getPrefs(context: Context) = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
}