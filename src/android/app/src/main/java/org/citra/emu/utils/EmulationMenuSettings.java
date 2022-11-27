package org.citra.emu.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.citra.emu.CitraApplication;
import org.citra.emu.overlay.InputOverlay;

public class EmulationMenuSettings {
    private static SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(CitraApplication.getAppContext());

    public static boolean getJoystickRelCenter() {
        return mPreferences.getBoolean(InputOverlay.PREF_JOYSTICK_RELATIVE, true);
    }

    public static boolean getDpadSlideEnable() {
        return mPreferences.getBoolean(InputOverlay.PREF_DPAD_SLIDE, true);
    }

    public static int getLandscapeScreenLayout() {
        return mPreferences.getInt(InputOverlay.PREF_SCREEN_LAYOUT, 0);
    }

    public static boolean getSwapScreens() {
        return mPreferences.getBoolean(InputOverlay.PREF_SWAP_SCREENS, false);
    }

    public static boolean getShowOverlay() {
        return mPreferences.getBoolean(InputOverlay.PREF_SHOW_OVERLAY, true);
    }
}
