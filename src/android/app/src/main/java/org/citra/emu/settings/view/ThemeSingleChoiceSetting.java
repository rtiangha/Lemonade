package org.citra.emu.settings.view;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.citra.emu.CitraApplication;
import org.citra.emu.R;
import org.citra.emu.settings.SettingsFragment;
import org.citra.emu.settings.model.Setting;

public final class ThemeSingleChoiceSetting extends SettingsItem {
    private int mDefaultValue;

    private int mChoicesId;
    private int mValuesId;
    private SettingsFragment mFragment;

    private static SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(CitraApplication.getAppContext());

    public ThemeSingleChoiceSetting(String key, String section, int titleId, int descriptionId,
                                      int choicesId, int valuesId, int defaultValue, Setting setting, SettingsFragment fragment) {
        super(key, section, setting, titleId, descriptionId);
        mValuesId = valuesId;
        mChoicesId = choicesId;
        mDefaultValue = defaultValue;
        mFragment = fragment;
    }

    public int getChoicesId() {
        return mChoicesId;
    }

    public int getValuesId() {
        return mValuesId;
    }

    public int getSelectedValue() {
        return mPreferences.getInt(getKey(), mDefaultValue);
    }

    /**
     * Write a value to the backing int. If that int was previously null,
     * initializes a new one and returns it, so it can be added to the Hashmap.
     *
     * @param selection New value of the int.
     * @return null if overwritten successfully otherwise; a newly created IntSetting.
     */
    public void setSelectedValue(int selection) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(getKey(), selection);
        editor.apply();
        mFragment.showToastMessage(CitraApplication.getAppContext().getString(R.string.design_updated), false);
    }

    @Override
    public int getType() {
        return TYPE_SINGLE_CHOICE;
    }
}
