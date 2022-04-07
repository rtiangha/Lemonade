package org.citra.emu.features.settings.model.view;

import org.citra.emu.features.settings.model.Setting;

public final class HeaderSetting extends SettingsItem {
    public HeaderSetting(String key, Setting setting, int titleId, int descriptionId) {
        super(key, null, setting, titleId, descriptionId);
    }

    @Override
    public int getType() {
        return SettingsItem.TYPE_HEADER;
    }
}
