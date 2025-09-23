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
    val view = ConstraintLayout(context).apply {
        setPadding(8.dp, 8.dp, 8.dp, 8.dp)
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    val icon = ImageView(context).apply {
        id = View.generateViewId()
        tag = "script_icon"
        layoutParams = ConstraintLayout.LayoutParams(48.dp, 48.dp).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(0xFFF0F0F0.toInt()) // Для отладки видимости
    }

    val textLayout = LinearLayout(context).apply {
        id = View.generateViewId()
        orientation = LinearLayout.VERTICAL
        layoutParams = ConstraintLayout.LayoutParams(0, WRAP_CONTENT).apply {
            startToEnd = icon.id
            endToStart = activeCheckBox.id
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = 8.dp
            marginEnd = 8.dp
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
        id = View.generateViewId()
        tag = "active_checkbox"
        contentDescription = "Активен"
        layoutParams = ConstraintLayout.LayoutParams(48.dp, WRAP_CONTENT).apply {
            startToEnd = textLayout.id
            endToStart = shortcutCheckBox.id
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        setBackgroundColor(0xFFF0F0F0.toInt()) // Для отладки
    }

    val shortcutCheckBox = CheckBox(context).apply {
        id = View.generateViewId()
        tag = "shortcut_checkbox"
        contentDescription = "Ярлык"
        layoutParams = ConstraintLayout.LayoutParams(48.dp, WRAP_CONTENT).apply {
            startToEnd = activeCheckBox.id
            endToStart = testButton.id
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        setBackgroundColor(0xFFF0F0F0.toInt()) // Для отладки
    }

    val testButton = Button(context).apply {
        id = View.generateViewId()
        tag = "test_button"
        text = "▶️"
        layoutParams = ConstraintLayout.LayoutParams(60.dp, WRAP_CONTENT).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        textSize = 12f
        visibility = View.VISIBLE
        setBackgroundColor(0xFFF0F0F0.toInt()) // Для отладки
    }

    view.addView(icon)
    view.addView(textLayout)
    view.addView(activeCheckBox)
    view.addView(shortcutCheckBox)
    view.addView(testButton)

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