package com.yourcompany.yourapp

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ScriptAdapter(
    private val context: Context,
    private val onSettingsClick: (Script) -> Unit,
    private val onTestClick: (Script) -> Unit
) : RecyclerView.Adapter<ScriptAdapter.ScriptViewHolder>() {

    private val scripts = mutableListOf<Script>()

    class ScriptViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewWithTag("script_icon")
        val name: TextView = view.findViewWithTag("script_name")
        val description: TextView = view.findViewWithTag("script_description")
        val activeCheckBox: CheckBox = view.findViewWithTag("active_checkbox")
        val shortcutCheckBox: CheckBox = view.findViewWithTag("shortcut_checkbox")
        val testButton: Button = view.findViewWithTag("test_button")
    }

override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScriptViewHolder {
    val view = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        
        // ФИКСИРОВАННЫЕ РАЗМЕРЫ КОЛОНОК
        val icon = ImageView(context).apply {
            tag = "script_icon"
            layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp) // ФИКС
        }
        
        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(200.dp, LinearLayout.LayoutParams.WRAP_CONTENT) // ФИКС
            addView(TextView(context).apply {
                tag = "script_name"
                textSize = 16f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            addView(TextView(context).apply {
                tag = "script_description" 
                textSize = 12f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
        }
        
        val activeCheckBox = CheckBox(context).apply {
            tag = "active_checkbox"
            layoutParams = LinearLayout.LayoutParams(48.dp, LinearLayout.LayoutParams.WRAP_CONTENT) // ФИКС
        }
        
        val shortcutCheckBox = CheckBox(context).apply {
            tag = "shortcut_checkbox" 
            layoutParams = LinearLayout.LayoutParams(48.dp, LinearLayout.LayoutParams.WRAP_CONTENT) // ФИКС
        }
        
        val testButton = Button(context).apply {
            tag = "test_button"
            text = "▶️"
            layoutParams = LinearLayout.LayoutParams(60.dp, LinearLayout.LayoutParams.WRAP_CONTENT) // ФИКС
            textSize = 12f
        }
        
        addView(icon)
        addView(textLayout) 
        addView(activeCheckBox)
        addView(shortcutCheckBox)
        addView(testButton)
    }
    return ScriptViewHolder(view)
}
override fun onBindViewHolder(holder: ScriptViewHolder, position: Int) {
    val script = scripts[position]
    val config = IniHelper.getScriptConfig(script.name)
    
    // Иконка
    val iconFile = File(Environment.getExternalStorageDirectory(), "MyScripts/icons/${config.icon}")
    if (config.icon.isNotEmpty() && iconFile.exists()) {
        holder.icon.setImageURI(Uri.fromFile(iconFile))
    } else {
        holder.icon.setImageResource(getIconResource(config.icon))
    }
    
    holder.name.text = config.name.ifEmpty { script.name }
    holder.description.text = config.description
    
    // Убираем принудительное показывание - пусть будут по умолчанию видны
    // holder.activeCheckBox.visibility = View.VISIBLE  // УБРАТЬ
    // holder.shortcutCheckBox.visibility = View.VISIBLE // УБРАТЬ
    
    // Сначала убираем слушателей чтобы избежать ложных срабатываний
    holder.activeCheckBox.setOnCheckedChangeListener(null)
    holder.shortcutCheckBox.setOnCheckedChangeListener(null)
    
    // Потом устанавливаем значения
    holder.activeCheckBox.isChecked = config.isActive
    holder.shortcutCheckBox.isChecked = config.hasShortcut
    
    // Теперь добавляем слушателей
    holder.activeCheckBox.setOnCheckedChangeListener { _, isChecked ->
        IniHelper.updateScriptConfig(script.name, config.copy(isActive = isChecked))
    }
    
    holder.shortcutCheckBox.setOnCheckedChangeListener { _, isChecked ->
        handleShortcutToggle(script, config, isChecked, holder)
    }
    
    holder.testButton.setOnClickListener { onTestClick(script) }
    holder.view.setOnLongClickListener {
        onSettingsClick(script)
        true
    }
}



// Новый метод для обработки переключения ярлыка
private fun handleShortcutToggle(script: Script, config: ScriptConfig, isChecked: Boolean, holder: ScriptViewHolder) {
    val shortcutName = config.name.ifEmpty { script.name }
    
    if (isChecked) {
        // Создание ярлыка
        TermuxHelper.createShortcut(
            context,
            shortcutName,
            script.path,
            "${context.packageName}.ShortcutActivity",
            getIconResource(config.icon)
        )
        IniHelper.updateScriptConfig(script.name, config.copy(hasShortcut = true))
    } else {
        // Удаление ярлыка - запускаем процесс с проверкой
        requestShortcutRemoval(script, config, holder)
    }
}

// Метод для запроса удаления ярлыка с проверкой
private fun requestShortcutRemoval(script: Script, config: ScriptConfig, holder: ScriptViewHolder) {
    val shortcutName = config.name.ifEmpty { script.name }
    
    // Отправляем команду удаления
    TermuxHelper.deleteShortcut(context, shortcutName, script.path)
    
    // Ждем немного и проверяем
    android.os.Handler().postDelayed({
        checkShortcutRemoval(script, config, holder, shortcutName)
    }, 1000)
}

// Проверка удаления ярлыка (заглушка - реальная проверка зависит от системы)
private fun checkShortcutRemoval(script: Script, config: ScriptConfig, holder: ScriptViewHolder, shortcutName: String) {
    // ЗДЕСЬ ДОЛЖНА БЫТЬ РЕАЛЬНАЯ ПРОВЕРКА СУЩЕСТВОВАНИЯ ЯРЛЫКА
    // Пока что просто обновляем состояние
    IniHelper.updateScriptConfig(script.name, config.copy(hasShortcut = false))
    
    // Альтернативно - показать диалог с инструкцией
    showShortcutRemovalInstructions(script, config, holder, shortcutName)
}

// Диалог с инструкциями по удалению
private fun showShortcutRemovalInstructions(script: Script, config: ScriptConfig, holder: ScriptViewHolder, shortcutName: String) {
    android.app.AlertDialog.Builder(context)
        .setTitle("Удаление ярлыка")
        .setMessage("Пожалуйста, удалите ярлык '$shortcutName' с рабочего стола вручную:\n\n1. Найдите ярлык на рабочем столе\n2. Долго нажмите на него\n3. Выберите 'Удалить' или перетащите в корзину")
        .setPositiveButton("Удалил") { _, _ ->
            // Пользователь говорит что удалил - обновляем статус
            IniHelper.updateScriptConfig(script.name, config.copy(hasShortcut = false))
            // TODO: здесь можно добавить реальную проверку и переспросить если ярлык еще есть
        }
        .setNegativeButton("Отмена") { _, _ ->
            // Возвращаем чекбокс обратно
            holder.shortcutCheckBox.isChecked = true
        }
        .show()
}




    override fun getItemCount(): Int = scripts.size

    fun updateScripts(newScripts: List<Script>) {
        scripts.clear()
        scripts.addAll(newScripts)
        notifyDataSetChanged()
    }

    private fun getIconResource(iconName: String): Int {
        return when (iconName) {
            "icon.png" -> R.mipmap.ic_launcher
            "Terminal.png" -> R.mipmap.ic_shortcut
            "no_icon.png" -> R.mipmap.ic_no_icon
            else -> R.mipmap.ic_no_icon
        }
    }

    private val Int.dp: Int
        get() = (this * context.resources.displayMetrics.density).toInt()
}

data class Script(val name: String, val path: String)
data class ScriptConfig(
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val isActive: Boolean = false,
    val hasShortcut: Boolean = false
)