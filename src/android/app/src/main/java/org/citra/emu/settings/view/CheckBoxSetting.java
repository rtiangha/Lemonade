package org.citra.emu.settings.view;

import org.citra.emu.CitraApplication;
import org.citra.emu.R;
import org.citra.emu.settings.SettingsFragment;
import org.citra.emu.settings.model.BooleanSetting;
import org.citra.emu.settings.model.IntSetting;
import org.citra.emu.settings.model.Setting;

public final class CheckBoxSetting extends SettingsItem {
    private boolean mDefaultValue;
    private boolean mShowPerformanceWarning;
    private SettingsFragment mFragment;

    public CheckBoxSetting(String key, String section, int titleId, int descriptionId,
                           boolean defaultValue, Setting setting) {
        super(key, section, setting, titleId, descriptionId);
        mDefaultValue = defaultValue;
        mShowPerformanceWarning = false;
    }

    public CheckBoxSetting(String key, String section, int titleId, int descriptionId,
                           boolean defaultValue, Setting setting, boolean show_performance_warning, SettingsFragment fragment) {
        super(key, section, setting, titleId, descriptionId);
        mDefaultValue = defaultValue;
        mFragment = fragment;
        mShowPerformanceWarning = show_performance_warning;
    }

    public boolean isChecked() {
        if (getSetting() == null) {
            return mDefaultValue;
        }

        // Try integer setting
        try {
            IntSetting setting = (IntSetting) getSetting();
            return setting.getValue() == 1;
        } catch (ClassCastException ignored) {
        }

        // Try boolean setting
        try {
            BooleanSetting setting = (BooleanSetting) getSetting();
            return setting.getValue() == true;
        } catch (ClassCastException ignored) {
        }

        return mDefaultValue;
    }

    /**
     * Write a value to the backing boolean. If that boolean was previously null,
     * initializes a new one and returns it, so it can be added to the Hashmap.
     *
     * @param checked Pretty self explanatory.
     * @return null if overwritten successfully; otherwise, a newly created BooleanSetting.
     */
    public IntSetting setChecked(boolean checked) {
        // Show a performance warning if the setting has been disabled
        if (mShowPerformanceWarning && !checked) {
            mFragment.showToastMessage(CitraApplication.getAppContext().getString(R.string.performance_warning), true);
        }

        if (getSetting() == null) {
            IntSetting setting = new IntSetting(getKey(), getSection(), checked ? 1 : 0);
            setSetting(setting);
            return setting;
        } else {
            IntSetting setting = (IntSetting) getSetting();
            setting.setValue(checked ? 1 : 0);
            return null;
        }
    }

    @Override
    public int getType() {
        return TYPE_CHECKBOX;
    }
}
