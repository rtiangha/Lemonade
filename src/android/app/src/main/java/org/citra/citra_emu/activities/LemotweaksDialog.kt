package org.citra.citra_emu.activities

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import org.citra.citra_emu.NativeLibrary
import org.citra.citra_emu.R
import java.util.ArrayList

class LemotweaksDialog : DialogFragment() {
    private lateinit var adapterId: SettingsAdapter

    companion object {
        const val SETTING_CORE_TICKS_HACK = 0
        const val SETTING_SKIP_SLOW_DRAW = 1
        const val SETTING_SKIP_TEXTURE_COPY = 2

        // view type
        const val TYPE_SWITCH = 0

        fun newInstance(): LemotweaksDialog {
            return LemotweaksDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val contents = requireActivity().layoutInflater.inflate(R.layout.dialog_lemotweaks, null) as ViewGroup

        val recyclerView: RecyclerView = contents.findViewById(R.id.list_settings)
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        adapterId = SettingsAdapter()
        recyclerView.adapter = adapterId
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        builder.setView(contents)
        return builder.create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        adapterId.saveSettings()
    }

    inner class SettingsItem(
        private val settingId: Int,
        private val nameId: String,
        private val typeId: Int,
        private var valueId: Int
    ) {
        fun getType(): Int {
            return typeId
        }

        fun getSetting(): Int {
            return settingId
        }

        fun getName(): String {
            return nameId
        }

        fun getValue(): Int {
            return valueId
        }

        fun setValue(value: Int) {
            valueId = value
        }
    }

    abstract class SettingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
            findViews(itemView)
        }

        protected abstract fun findViews(root: View)
        abstract fun bind(item: SettingsItem)
        override fun onClick(clicked: View) {
            // Handle click event
        }
    }

    inner class SwitchSettingViewHolder(itemView: View) : SettingViewHolder(itemView), CompoundButton.OnCheckedChangeListener {
        private var itemId: SettingsItem? = null
        private var textSettingNameId: TextView? = null
        private var switchId: MaterialSwitch? = null

        init {
            findViews(itemView)
        }

        override fun findViews(root: View) {
            textSettingNameId = root.findViewById(R.id.text_setting_name)
            switchId = root.findViewById(R.id.switch_widget)
            switchId?.setOnCheckedChangeListener(this)
        }

        override fun bind(item: SettingsItem) {
            itemId = item
            textSettingNameId?.text = item.getName()
            switchId?.isChecked = item.getValue() > 0
        }

        override fun onClick(clicked: View) {
            switchId?.toggle()
            itemId?.setValue(if (switchId?.isChecked == true) 1 else 0)
        }

        override fun onCheckedChanged(view: CompoundButton, isChecked: Boolean) {
            itemId?.setValue(if (isChecked) 1 else 0)
        }
    }

    inner class SettingsAdapter : RecyclerView.Adapter<SettingViewHolder>() {
        private var runningSettingsId: IntArray
        private var settingsId: ArrayList<SettingsItem>

        init {
            var i = 0
            runningSettingsId = NativeLibrary.getRunningSettings()
            settingsId = ArrayList()

            // native settings
            settingsId.add(SettingsItem(SETTING_CORE_TICKS_HACK, getString(R.string.setting_core_ticks_hack), TYPE_SWITCH, runningSettingsId[i++]))
            settingsId.add(SettingsItem(SETTING_SKIP_SLOW_DRAW, getString(R.string.setting_skip_slow_draw), TYPE_SWITCH, runningSettingsId[i++]))
            settingsId.add(SettingsItem(SETTING_SKIP_TEXTURE_COPY, getString(R.string.setting_skip_texture_copy), TYPE_SWITCH, runningSettingsId[i++]))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                TYPE_SWITCH -> {
                    val itemView = inflater.inflate(R.layout.list_item_running_switch, parent, false)
                    SwitchSettingViewHolder(itemView)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun getItemCount(): Int {
            return settingsId.size
        }

        override fun getItemViewType(position: Int): Int {
            return settingsId[position].getType()
        }

        override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
            holder.bind(settingsId[position])
        }

        fun saveSettings() {
            // native settings
            var isChanged = false
            val newSettings = IntArray(runningSettingsId.size)
            for (i in runningSettingsId.indices) {
                newSettings[i] = settingsId[i].getValue()
                if (newSettings[i] != runningSettingsId[i]) {
                    isChanged = true
                }
            }
            // apply settings if changes are detected
            if (isChanged) {
                NativeLibrary.setRunningSettings(newSettings)
            }
        }
    }
}
