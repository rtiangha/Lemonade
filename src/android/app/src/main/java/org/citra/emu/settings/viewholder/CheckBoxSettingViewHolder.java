package org.citra.emu.settings.viewholder;

import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import org.citra.emu.R;
import org.citra.emu.settings.view.CheckBoxSetting;
import org.citra.emu.settings.view.SettingsItem;
import org.citra.emu.settings.SettingsAdapter;

public final class CheckBoxSettingViewHolder extends SettingViewHolder {
    private CheckBoxSetting mItem;

    private TextView mTextSettingName;
    private TextView mTextSettingDescription;

    private Switch mSwitch;

    public CheckBoxSettingViewHolder(View itemView, SettingsAdapter adapter) {
        super(itemView, adapter);
    }

    @Override
    protected void findViews(View root) {
        mTextSettingName = root.findViewById(R.id.text_setting_name);
        mTextSettingDescription = root.findViewById(R.id.text_setting_description);
        mSwitch = root.findViewById(R.id.checkbox);
    }

    @Override
    public void bind(SettingsItem item) {
        mItem = (CheckBoxSetting) item;

        mTextSettingName.setText(item.getNameId());

        if (item.getDescriptionId() > 0) {
            mTextSettingDescription.setText(item.getDescriptionId());
            mTextSettingDescription.setVisibility(View.VISIBLE);
        } else {
            mTextSettingDescription.setText("");
            mTextSettingDescription.setVisibility(View.GONE);
        }

        mSwitch.setChecked(mItem.isChecked());
    }

    @Override
    public void onClick(View clicked) {
        mSwitch.toggle();

        getAdapter().onBooleanClick(mItem, getAdapterPosition(), mSwitch.isChecked());
    }
}
