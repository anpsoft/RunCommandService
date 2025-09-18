package com.yourcompany.yourapp

import android.content.Context
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
            val icon = ImageView(context).apply {
                tag = "script_icon"
                layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)
            }
            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    tag = "script_name"
                    textSize = 16f
                })
                addView(TextView(context).apply {
                    tag = "script_description"
                    textSize = 12f
                })
            }
            val activeCheckBox = CheckBox(context).apply {
                tag = "active_checkbox"
                contentDescription = "Активен"
            }
            val shortcutCheckBox = CheckBox(context).apply {
                tag = "shortcut_checkbox"
                contentDescription = "Ярлык"
            }
            val testButton = Button(context).apply {
                tag = "test_button"
                text = "▶️"
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    width = 0
                    minimumWidth = 0
                    setPadding(4.dp, 4.dp, 4.dp, 4.dp)
                }
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

        holder.icon.setImageResource(
            if (config.icon.isNotEmpty()) R.mipmap.ic_no_icon else R.mipmap.ic_no_icon
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