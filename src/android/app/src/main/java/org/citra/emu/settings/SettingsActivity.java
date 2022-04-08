package org.citra.emu.settings;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.citra.emu.NativeLibrary;
import org.citra.emu.R;
import org.citra.emu.utils.DirectoryInitialization;
import org.citra.emu.utils.DirectoryInitialization.DirectoryStateReceiver;
import org.citra.emu.utils.EmulationMenuSettings;
import org.citra.emu.utils.Log;
import org.citra.emu.utils.ThemeUtil;

import java.io.File;

public final class SettingsActivity extends AppCompatActivity {
    private static final String ARG_MENU_TAG = "menu_tag";
    private static final String ARG_GAME_ID = "game_id";
    private static final String FRAGMENT_TAG = "settings";
    // key
    private static final String KEY_SHOULD_SAVE = "should_save";

    private org.citra.emu.settings.model.Settings mSettings = new org.citra.emu.settings.model.Settings();

    private boolean mShouldSave;

    private DirectoryStateReceiver directoryStateReceiver;

    private String menuTag;
    private String gameId;

    private ProgressDialog dialog;

    public static void launch(Context context, String menuTag, String gameId) {
        Intent settings = new Intent(context, SettingsActivity.class);
        settings.putExtra(ARG_MENU_TAG, menuTag);
        settings.putExtra(ARG_GAME_ID, gameId);
        context.startActivity(settings);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Intent launcher = getIntent();
        String gameID = launcher.getStringExtra(ARG_GAME_ID);
        String menuTag = launcher.getStringExtra(ARG_MENU_TAG);

        onCreate(savedInstanceState, menuTag, gameID);

        // Show "Back" button in the action bar for navigation
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // Critical: If super method is not called, rotations will be busted.
        super.onSaveInstanceState(outState);
        saveState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        prepareCitraDirectoriesIfNeeded();
    }

    /**
     * If this is called, the user has left the settings screen (potentially through the
     * home button) and will expect their changes to be persisted. So we kick off an
     * IntentService which will do so on a background thread.
     */
    @Override
    protected void onStop() {
        super.onStop();

        onStopActivity(isFinishing());

        // Update framebuffer layout when closing the settings
        NativeLibrary.NotifyOrientationChange(EmulationMenuSettings.getLandscapeScreenLayout(),
                getWindowManager().getDefaultDisplay().getRotation());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        return true;
    }

    public void showSettingsFragment(String menuTag, boolean addToStack, String gameID) {
        if (!addToStack && getFragment() != null) {
            return;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (addToStack) {
            if (areSystemAnimationsEnabled()) {
                transaction.setCustomAnimations(
                        R.animator.settings_enter,
                        R.animator.settings_exit,
                        R.animator.settings_pop_enter,
                        R.animator.setttings_pop_exit);
            }

            transaction.addToBackStack(null);
        }
        transaction.replace(R.id.frame_content, SettingsFragment.newInstance(menuTag, gameID), FRAGMENT_TAG);

        transaction.commit();
    }

    private boolean areSystemAnimationsEnabled() {
        float duration = Settings.Global.getFloat(
                getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1);
        float transition = Settings.Global.getFloat(
                getContentResolver(),
                Settings.Global.TRANSITION_ANIMATION_SCALE, 1);
        return duration != 0 && transition != 0;
    }

    public void onCreate(Bundle savedInstanceState, String menuTag, String gameId) {
        if (savedInstanceState == null) {
            this.menuTag = menuTag;
            this.gameId = gameId;
        } else {
            mShouldSave = savedInstanceState.getBoolean(KEY_SHOULD_SAVE);
        }
    }

    void loadSettingsUI() {
        if (mSettings.isEmpty()) {
            if (!TextUtils.isEmpty(gameId)) {
                mSettings.loadSettings(gameId, this);
            } else {
                mSettings.loadSettings(this);
            }
        }

        showSettingsFragment(menuTag, false, gameId);
        onSettingsFileLoaded(mSettings);
    }

    private void prepareCitraDirectoriesIfNeeded() {
        File configFile = new File(DirectoryInitialization.getUserDirectory() + "/config/" + SettingsFile.FILE_NAME_CONFIG + ".ini");
        if (!configFile.exists()) {
            Log.error("Citra config file could not be found!");
        }
        if (DirectoryInitialization.areCitraDirectoriesReady()) {
            loadSettingsUI();
        } else {
            showLoading();
            IntentFilter statusIntentFilter = new IntentFilter(
                    DirectoryInitialization.BROADCAST_ACTION);

            directoryStateReceiver =
                    new DirectoryStateReceiver(directoryInitializationState ->
                    {
                        if (directoryInitializationState == DirectoryInitialization.DirectoryInitializationState.CITRA_DIRECTORIES_INITIALIZED) {
                            hideLoading();
                            loadSettingsUI();
                        } else if (directoryInitializationState == DirectoryInitialization.DirectoryInitializationState.EXTERNAL_STORAGE_PERMISSION_NEEDED) {
                            showPermissionNeededHint();
                            hideLoading();
                        } else if (directoryInitializationState == DirectoryInitialization.DirectoryInitializationState.CANT_FIND_EXTERNAL_STORAGE) {
                            showExternalStorageNotMountedHint();
                            hideLoading();
                        }
                    });

            startDirectoryInitializationService(directoryStateReceiver, statusIntentFilter);
        }
    }

    public void setSettings(org.citra.emu.settings.model.Settings settings) {
        mSettings = settings;
    }

    public org.citra.emu.settings.model.Settings getSettings() {
        return mSettings;
    }

    public void onStopActivity(boolean finishing) {
        if (directoryStateReceiver != null) {
            stopListeningToDirectoryInitializationService(directoryStateReceiver);
            directoryStateReceiver = null;
        }

        if (mSettings != null && finishing && mShouldSave) {
            Log.debug("[SettingsActivity] Settings activity stopping. Saving settings to INI...");
            mSettings.saveSettings(this);
        }

        ThemeUtil.applyTheme();

        NativeLibrary.ReloadSettings();
    }

    public void onSettingChanged() {
        mShouldSave = true;
    }

    public void saveState(Bundle outState) {
        outState.putBoolean(KEY_SHOULD_SAVE, mShouldSave);
    }

    public void startDirectoryInitializationService(DirectoryStateReceiver receiver, IntentFilter filter) {
        LocalBroadcastManager.getInstance(this).registerReceiver(
                receiver,
                filter);
        DirectoryInitialization.start(this);
    }

    public void stopListeningToDirectoryInitializationService(DirectoryStateReceiver receiver) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    public void showLoading() {
        if (dialog == null) {
            dialog = new ProgressDialog(this);
            dialog.setMessage(getString(R.string.load_settings));
            dialog.setIndeterminate(true);
        }

        dialog.show();
    }

    public void hideLoading() {
        dialog.dismiss();
    }

    public void showPermissionNeededHint() {
        Toast.makeText(this, R.string.write_permission_needed, Toast.LENGTH_SHORT)
                .show();
    }

    public void showExternalStorageNotMountedHint() {
        Toast.makeText(this, R.string.external_storage_not_mounted, Toast.LENGTH_SHORT)
                .show();
    }

    public void onSettingsFileLoaded(org.citra.emu.settings.model.Settings settings) {
        SettingsFragment fragment = getFragment();

        if (fragment != null) {
            fragment.onSettingsFileLoaded(settings);
        }
    }

    public void onSettingsFileNotFound() {
        SettingsFragment fragment = getFragment();

        if (fragment != null) {
            fragment.loadDefaultSettings();
        }
    }

    public void showToastMessage(String message, boolean is_long) {
        Toast.makeText(this, message, is_long ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    private SettingsFragment getFragment() {
        return (SettingsFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    }
}
