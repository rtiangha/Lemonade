package org.citra.citra_emu.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import org.citra.citra_emu.NativeLibrary;
import org.citra.citra_emu.R;
import org.citra.citra_emu.dialogs.RunningSettingDialog;
import org.citra.citra_emu.features.settings.model.IntSetting;
import org.citra.citra_emu.features.settings.model.Settings;
import org.citra.citra_emu.features.settings.model.view.InputBindingSetting;
import org.citra.citra_emu.features.settings.utils.SettingsFile;
import org.citra.citra_emu.camera.StillImageCameraHelper;
import org.citra.citra_emu.fragments.EmulationFragment;
import org.citra.citra_emu.ui.main.MainActivity;
import org.citra.citra_emu.utils.ControllerMappingHelper;
import org.citra.citra_emu.utils.EmulationMenuSettings;
import org.citra.citra_emu.utils.FileBrowserHelper;
import org.citra.citra_emu.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.RECORD_AUDIO;

public final class EmulationActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTED_GAME = "SelectedGame";
    public static final String EXTRA_SELECTED_TITLE = "SelectedTitle";

    public static final int REQUEST_SELECT_AMIIBO = 2;

    private static WeakReference<EmulationActivity> sInstance = new WeakReference<>(null);

    private EmulationFragment mEmulationFragment;
    private SharedPreferences mPreferences;
    private ControllerMappingHelper mControllerMappingHelper;
    private boolean activityRecreated;
    private boolean mMenuVisible;
    private String mSelectedTitle;
    private String mPath;

    public static void launch(FragmentActivity activity, String path, String title) {
        Intent launcher = new Intent(activity, EmulationActivity.class);

        launcher.putExtra(EXTRA_SELECTED_GAME, path);
        launcher.putExtra(EXTRA_SELECTED_TITLE, title);
        activity.startActivity(launcher);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = new WeakReference<>(this);

        if (savedInstanceState == null) {
            // Get params we were passed
            Intent gameToEmulate = getIntent();
            mPath = gameToEmulate.getStringExtra(EXTRA_SELECTED_GAME);
            mSelectedTitle = gameToEmulate.getStringExtra(EXTRA_SELECTED_TITLE);
            activityRecreated = false;
        } else {
            activityRecreated = true;
            restoreState(savedInstanceState);
        }

        mControllerMappingHelper = new ControllerMappingHelper();

        // only android 9+ support this feature.
        Settings settings = new Settings();
        settings.loadSettings(null);
        IntSetting expandToCutoutAreaSetting =
                (IntSetting) settings.getSection(Settings.SECTION_INTERFACE)
                        .getSetting(SettingsFile.KEY_EXPAND_TO_CUTOUT_AREA);
        int expandToCutoutArea = expandToCutoutAreaSetting.getValue();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && expandToCutoutArea == 1) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setTheme(R.style.CitraEmulationBase);

        setContentView(R.layout.activity_emulation);

        // Find or create the EmulationFragment
        mEmulationFragment = (EmulationFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frame_emulation_fragment);
        if (mEmulationFragment == null) {
            mEmulationFragment = EmulationFragment.newInstance(mPath);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frame_emulation_fragment, mEmulationFragment)
                    .commit();
        }

        setTitle(mSelectedTitle);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Override Citra core INI with the one set by our in game menu
        NativeLibrary.SwapScreens(EmulationMenuSettings.getSwapScreens(),
                getWindowManager().getDefaultDisplay().getRotation());
    }

    public static EmulationActivity get() {
        return sInstance.get();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(EXTRA_SELECTED_GAME, mPath);
        outState.putString(EXTRA_SELECTED_TITLE, mSelectedTitle);
        super.onSaveInstanceState(outState);
    }

    protected void restoreState(Bundle savedInstanceState) {
        mPath = savedInstanceState.getString(EXTRA_SELECTED_GAME);
        mSelectedTitle = savedInstanceState.getString(EXTRA_SELECTED_TITLE);

        // If an alert prompt was in progress when state was restored, retry displaying it
        NativeLibrary.retryDisplayAlertPrompt();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        NativeLibrary.ReloadCameraDevices();
    }

    @Override
    public void onBackPressed() {
        if (mMenuVisible) {
            mEmulationFragment.stopEmulation();
            finish();
            sInstance = new WeakReference<>(null);
        } else {
            mMenuVisible = true;
            mEmulationFragment.stopConfiguringControls();
            RunningSettingDialog dialog = RunningSettingDialog.newInstance();
            dialog.show(getSupportFragmentManager(), "RunningSettingDialog");
            dialog.setOnDismissListener(v -> {
                mMenuVisible = false;
                hideSystemUI();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case NativeLibrary.REQUEST_CODE_NATIVE_CAMERA:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED &&
                        shouldShowRequestPermissionRationale(CAMERA)) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.camera)
                            .setMessage(R.string.camera_permission_needed)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                NativeLibrary.CameraPermissionResult(grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
            case NativeLibrary.REQUEST_CODE_NATIVE_MIC:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED &&
                        shouldShowRequestPermissionRationale(RECORD_AUDIO)) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.microphone)
                            .setMessage(R.string.microphone_permission_needed)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                NativeLibrary.MicPermissionResult(grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    public void loadAmiibo() {
        FileBrowserHelper.openFilePicker(this, REQUEST_SELECT_AMIIBO,
                R.string.select_amiibo,
                Collections.singletonList("bin"), false);
    }

    public void stopEmulation() {
        mEmulationFragment.stopEmulation();
    }

    public void rotateScreen() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    public void editControlsPlacement() {
        if (mEmulationFragment.isConfiguringControls()) {
            mEmulationFragment.stopConfiguringControls();
        } else {
            mEmulationFragment.startConfiguringControls();
        }
    }

    // Gets button presses
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mMenuVisible) {
            return super.dispatchKeyEvent(event);
        }

        int action;
        int button = mPreferences.getInt(InputBindingSetting.getInputButtonKey(event.getKeyCode()), event.getKeyCode());

        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                // Handling the case where the back button is pressed.
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    onBackPressed();
                    return true;
                }

                // Normal key events.
                action = NativeLibrary.ButtonState.PRESSED;
                break;
            case KeyEvent.ACTION_UP:
                action = NativeLibrary.ButtonState.RELEASED;
                break;
            default:
                return false;
        }
        InputDevice input = event.getDevice();

        if (input == null) {
            // Controller was disconnected
            return false;
        }

        return NativeLibrary.onGamePadEvent(input.getDescriptor(), button, action);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        switch (requestCode) {
            case StillImageCameraHelper.REQUEST_CAMERA_FILE_PICKER:
                StillImageCameraHelper.OnFilePickerResult(resultCode == RESULT_OK ? result : null);
                break;
            case REQUEST_SELECT_AMIIBO:
                // If the user picked a file, as opposed to just backing out.
                if (resultCode == MainActivity.RESULT_OK) {
                    String[] selectedFiles = FileBrowserHelper.getSelectedFiles(result);
                    if (selectedFiles == null)
                        return;

                    onAmiiboSelected(selectedFiles[0]);
                }
                break;
        }
    }

    private void onAmiiboSelected(String selectedFile) {
        File file = new File(selectedFile);
        boolean success = false;
        try {
            byte[] bytes = FileUtil.getBytesFromFile(file);
            success = NativeLibrary.LoadAmiibo(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!success) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.amiibo_load_error)
                    .setMessage(R.string.amiibo_load_error_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();
        }
    }

    public void removeAmiibo() {
        NativeLibrary.RemoveAmiibo();
    }

    public String getGameTitle() {
        return mSelectedTitle;
    }

    public void toggleControls() {
        final SharedPreferences.Editor editor = mPreferences.edit();
        boolean[] enabledButtons = new boolean[14];
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.emulation_toggle_controls);

        for (int i = 0; i < enabledButtons.length; i++) {
            // Buttons that are disabled by default
            boolean defaultValue = true;
            switch (i) {
                case 6: // ZL
                case 7: // ZR
                case 12: // C-stick
                    defaultValue = false;
                    break;
            }

            enabledButtons[i] = mPreferences.getBoolean("buttonToggle" + i, defaultValue);
        }
        builder.setMultiChoiceItems(R.array.n3dsButtons, enabledButtons,
                (dialog, indexSelected, isChecked) -> editor
                        .putBoolean("buttonToggle" + indexSelected, isChecked));
        builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) ->
        {
            editor.apply();

            mEmulationFragment.refreshInputOverlay();
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void resetOverlay() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.emulation_touch_overlay_reset))
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> mEmulationFragment.resetInputOverlay())
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                })
                .create()
                .show();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (mMenuVisible) {
            return false;
        }

        if (((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) == 0)) {
            return super.dispatchGenericMotionEvent(event);
        }

        // Don't attempt to do anything if we are disconnecting a device.
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            return true;
        }

        InputDevice input = event.getDevice();
        List<InputDevice.MotionRange> motions = input.getMotionRanges();

        float[] axisValuesCirclePad = {0.0f, 0.0f};
        float[] axisValuesCStick = {0.0f, 0.0f};
        float[] axisValuesDPad = {0.0f, 0.0f};
        boolean isTriggerPressedLMapped = false;
        boolean isTriggerPressedRMapped = false;
        boolean isTriggerPressedZLMapped = false;
        boolean isTriggerPressedZRMapped = false;
        boolean isTriggerPressedL = false;
        boolean isTriggerPressedR = false;
        boolean isTriggerPressedZL = false;
        boolean isTriggerPressedZR = false;

        for (InputDevice.MotionRange range : motions) {
            int axis = range.getAxis();
            float origValue = event.getAxisValue(axis);
            float value = mControllerMappingHelper.scaleAxis(input, axis, origValue);
            int nextMapping = mPreferences.getInt(InputBindingSetting.getInputAxisButtonKey(axis), -1);
            int guestOrientation = mPreferences.getInt(InputBindingSetting.getInputAxisOrientationKey(axis), -1);

            if (nextMapping == -1 || guestOrientation == -1) {
                // Axis is unmapped
                continue;
            }

            if ((value > 0.f && value < 0.1f) || (value < 0.f && value > -0.1f)) {
                // Skip joystick wobble
                value = 0.f;
            }

            if (nextMapping == NativeLibrary.ButtonType.STICK_LEFT) {
                axisValuesCirclePad[guestOrientation] = value;
            } else if (nextMapping == NativeLibrary.ButtonType.STICK_C) {
                axisValuesCStick[guestOrientation] = value;
            } else if (nextMapping == NativeLibrary.ButtonType.DPAD) {
                axisValuesDPad[guestOrientation] = value;
            } else if (nextMapping == NativeLibrary.ButtonType.TRIGGER_L) {
                isTriggerPressedLMapped = true;
                isTriggerPressedL = value != 0.f;
            } else if (nextMapping == NativeLibrary.ButtonType.TRIGGER_R) {
                isTriggerPressedRMapped = true;
                isTriggerPressedR = value != 0.f;
            } else if (nextMapping == NativeLibrary.ButtonType.BUTTON_ZL) {
                isTriggerPressedZLMapped = true;
                isTriggerPressedZL = value != 0.f;
            } else if (nextMapping == NativeLibrary.ButtonType.BUTTON_ZR) {
                isTriggerPressedZRMapped = true;
                isTriggerPressedZR = value != 0.f;
            }
        }

        // Circle-Pad and C-Stick status
        NativeLibrary.onGamePadMoveEvent(input.getDescriptor(), NativeLibrary.ButtonType.STICK_LEFT, axisValuesCirclePad[0], axisValuesCirclePad[1]);
        NativeLibrary.onGamePadMoveEvent(input.getDescriptor(), NativeLibrary.ButtonType.STICK_C, axisValuesCStick[0], axisValuesCStick[1]);

        // Triggers L/R and ZL/ZR
        if (isTriggerPressedLMapped) {
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.TRIGGER_L, isTriggerPressedL ? NativeLibrary.ButtonState.PRESSED : NativeLibrary.ButtonState.RELEASED);
        }
        if (isTriggerPressedRMapped) {
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.TRIGGER_R, isTriggerPressedR ? NativeLibrary.ButtonState.PRESSED : NativeLibrary.ButtonState.RELEASED);
        }
        if (isTriggerPressedZLMapped) {
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.BUTTON_ZL, isTriggerPressedZL ? NativeLibrary.ButtonState.PRESSED : NativeLibrary.ButtonState.RELEASED);
        }
        if (isTriggerPressedZRMapped) {
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.BUTTON_ZR, isTriggerPressedZR ? NativeLibrary.ButtonState.PRESSED : NativeLibrary.ButtonState.RELEASED);
        }

        // Work-around to allow D-pad axis to be bound to emulated buttons
        if (axisValuesDPad[0] == 0.f) {
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.DPAD_LEFT, NativeLibrary.ButtonState.RELEASED);
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.DPAD_RIGHT, NativeLibrary.ButtonState.RELEASED);
        }
        if (axisValuesDPad[0] < 0.f) {
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.DPAD_LEFT, NativeLibrary.ButtonState.PRESSED);
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.DPAD_RIGHT, NativeLibrary.ButtonState.RELEASED);
        }
        if (axisValuesDPad[0] > 0.f) {
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.DPAD_LEFT, NativeLibrary.ButtonState.RELEASED);
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.DPAD_RIGHT, NativeLibrary.ButtonState.PRESSED);
        }
        if (axisValuesDPad[1] == 0.f) {
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.DPAD_UP, NativeLibrary.ButtonState.RELEASED);
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.DPAD_DOWN, NativeLibrary.ButtonState.RELEASED);
        }
        if (axisValuesDPad[1] < 0.f) {
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.DPAD_UP, NativeLibrary.ButtonState.PRESSED);
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.DPAD_DOWN, NativeLibrary.ButtonState.RELEASED);
        }
        if (axisValuesDPad[1] > 0.f) {
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.DPAD_UP, NativeLibrary.ButtonState.RELEASED);
            NativeLibrary.onGamePadEvent(NativeLibrary.TouchScreenDevice, NativeLibrary.ButtonType.DPAD_DOWN, NativeLibrary.ButtonState.PRESSED);
        }

        return true;
    }

    public void refreshControls() {
        mEmulationFragment.refreshControls();
    }

    public boolean isActivityRecreated() {
        return activityRecreated;
    }
}
