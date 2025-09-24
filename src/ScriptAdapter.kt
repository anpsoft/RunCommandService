package com.yourcompany.yourapp5

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.Gravity
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
            setPadding(4, 4, 4, 4)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val icon = ImageView(context).apply {
            tag = "script_icon"
            layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp).apply {
                width = 48.dp
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(0xFFF0F0F0.toInt())
        }

        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(4, 0, 4, 0)

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
            layoutParams = LinearLayout.LayoutParams(48.dp, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                width = 48.dp
            }
            setBackgroundColor(0xFFF0F0F0.toInt())
        }

        val shortcutCheckBox = CheckBox(context).apply {
            tag = "shortcut_checkbox"
            contentDescription = "Ярлык"
            layoutParams = LinearLayout.LayoutParams(48.dp, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                width = 48.dp
            }
            setBackgroundColor(0xFFF0F0F0.toInt())
        }

        val testButton = Button(context).apply {
            tag = "test_button"
            text = "▶️"
            layoutParams = LinearLayout.LayoutParams(60.dp, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                width = 60.dp
            }
            textSize = 12f
            visibility = View.VISIBLE
            setBackgroundColor(0xFFF0F0F0.toInt())
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

        //val iconFile = File(Environment.getExternalStorageDirectory(), "MyScripts/icons/${config.icon}")
        

        val iconFile = File(IniHelper.getIconsDir(context), config.icon)
        
        if (config.icon.isNotEmpty() && iconFile.exists()) {
            holder.icon.setImageURI(Uri.fromFile(iconFile))
        } else {
            holder.icon.setImageResource(ShortcutManager.getIconResource(config.icon))
        }
        
        holder.name.text = config.name.ifEmpty { script.name }
        holder.description.text = config.description
        
        holder.activeCheckBox.setOnCheckedChangeListener(null)
        holder.shortcutCheckBox.setOnCheckedChangeListener(null)
        
        holder.activeCheckBox.isChecked = config.isActive
        holder.shortcutCheckBox.isChecked = config.hasShortcut
        
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
        // Показываем диалог вместо удаления
        android.app.AlertDialog.Builder(context)
            .setTitle("Удаление ярлыка")
            .setMessage("Android 7 не позволяет программно удалять ярлыки. Удалите ярлык '${config.name.ifEmpty { script.name }}' с рабочего стола вручную.")
            .setPositiveButton("Я удалил") { _, _ ->
                IniHelper.updateScriptConfig(script.name, config.copy(hasShortcut = false))
            }
            .setNegativeButton("Отменить") { _, _ ->
                holder.shortcutCheckBox.isChecked = true
            }
            .show()
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