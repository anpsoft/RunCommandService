package com.yourcompany.yourapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
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
        val icon: ImageView = view.findViewById(R.id.script_icon)
        val name: TextView = view.findViewById(R.id.script_name)
        val description: TextView = view.findViewById(R.id.script_description)
        val activeCheckBox: CheckBox = view.findViewById(R.id.active_checkbox)
        val shortcutCheckBox: CheckBox = view.findViewById(R.id.shortcut_checkbox)
        val testButton: Button = view.findViewById(R.id.test_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScriptViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.script_item, parent, false)
        return ScriptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScriptViewHolder, position: Int) {
        val script = scripts[position]
        val config = IniHelper.getScriptConfig(script.name)

        holder.icon.setImageResource(
            if (config.icon.isNotEmpty()) {
                R.mipmap.ic_no_icon // Пока заглушка
            } else {
                R.mipmap.ic_no_icon
            }
        )
        holder.name.text = config.name.ifEmpty { script.name }
        holder.description.text = config.description
        holder.activeCheckBox.isChecked = config.isActive
        holder.shortcutCheckBox.isChecked = config.hasShortcut

        holder.activeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            IniHelper.updateScriptConfig(script.name, config.copy(isActive = isChecked))
        }
        holder.shortcutCheckBox.setOnCheckedChangeListener { _, isChecked ->
            val shortcutName = config.name.ifEmpty { script.name }
            if (isChecked) {
                TermuxHelper.createShortcut(
                    context,
                    shortcutName,
                    script.path,
                    "${context.packageName}.ShortcutActivity",
                    R.mipmap.ic_shortcut
                )
            } else {
                TermuxHelper.deleteShortcut(context, shortcutName, script.path)
            }
            IniHelper.updateScriptConfig(script.name, config.copy(hasShortcut = isChecked))
        }
        holder.testButton.text = "▶️"
        holder.testButton.setOnClickListener { onTestClick(script) }
        holder.view.setOnLongClickListener {
            onSettingsClick(script)
            true
        }
    }

    override fun getItemCount(): Int = scripts.size

    fun updateScripts(newScripts: List<Script>) {
        scripts.clear()
        scripts.addAll(newScripts)
        notifyDataSetChanged()
    }
}

data class Script(val name: String, val path: String)
data class ScriptConfig(
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val isActive: Boolean = true,
    val hasShortcut: Boolean = false
)