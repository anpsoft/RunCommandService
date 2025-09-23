package com.yourcompany.yourapp

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.view.View
import android.view.ViewGroup

import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
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
    val view = TableLayout(context).apply {
        setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        isStretchAllColumns = false  // Отключаем растяжение всех столбцов
    }

    val row = TableRow(context).apply {
        layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )

        val icon = ImageView(context).apply {
            tag = "script_icon"
            layoutParams = TableRow.LayoutParams(48.dp, 48.dp)
            scaleType = ImageView.ScaleType.CENTER_CROP
            gravity = Gravity.CENTER
        }

        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT).apply {
                weight = 1f  // Только эта колонка растягивается
            }
            setPadding(8.dp, 0, 8.dp, 0)

            addView(TextView(context).apply {
                tag = "script_name"
                textSize = 16f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTypeface(null, android.graphics.Typeface.BOLD)
            })
            addView(TextView(context).apply {
                tag = "script_description"
                textSize = 12f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(0xFF666666.toInt())
            })
        }

        val activeCheckBox = CheckBox(context).apply {
            tag = "active_checkbox"
            contentDescription = "Активен"
            layoutParams = TableRow.LayoutParams(48.dp, TableRow.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER
        }

        val shortcutCheckBox = CheckBox(context).apply {
            tag = "shortcut_checkbox"
            contentDescription = "Ярлык"
            layoutParams = TableRow.LayoutParams(48.dp, TableRow.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER
        }

        val testButton = Button(context).apply {
            tag = "test_button"
            text = "▶️"
            layoutParams = TableRow.LayoutParams(60.dp, TableRow.LayoutParams.WRAP_CONTENT)
            textSize = 12f
            gravity = Gravity.CENTER
            visibility = View.VISIBLE
        }

        addView(icon)              // Фиксированная, слева
        addView(textLayout)        // Растягивается
        addView(activeCheckBox)    // Фиксированная, справа
        addView(shortcutCheckBox)  // Фиксированная, справа
        addView(testButton)        // Фиксированная, справа
    }

    view.addView(row)
    return ScriptViewHolder(view)
}



    override fun onBindViewHolder(holder: ScriptViewHolder, position: Int) {
        val script = scripts[position]
        val config = IniHelper.getScriptConfig(script.name)

        val iconFile = File(Environment.getExternalStorageDirectory(), "MyScripts/icons/${config.icon}")
        if (config.icon.isNotEmpty() && iconFile.exists()) {
            holder.icon.setImageURI(Uri.fromFile(iconFile))
        } else {
            holder.icon.setImageResource(ShortcutManager.getIconResource(config.icon))
        }
        
        holder.name.text = config.name.ifEmpty { script.name }
        holder.description.text = config.description
        
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

    private fun handleShortcutToggle(script: Script, config: ScriptConfig, isChecked: Boolean, holder: ScriptViewHolder) {
        if (isChecked) {
            ShortcutManager.createShortcut(context, script.name, script.path, config.icon)
        } else {
            ShortcutManager.deleteShortcut(context, script.name, script.path)
        }
    }

    override fun getItemCount(): Int = scripts.size

    fun updateScripts(newScripts: List<Script>) {
        scripts.clear()
        scripts.addAll(newScripts)
        notifyDataSetChanged()
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