package org.citra.citra_emu.ui.main;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.citra.citra_emu.BuildConfig;
import org.citra.citra_emu.CitraApplication;
import org.citra.citra_emu.NativeLibrary;
import org.citra.citra_emu.R;
import org.citra.citra_emu.activities.EmulationActivity;
import org.citra.citra_emu.dialogs.CreditsDialog;
import org.citra.citra_emu.features.settings.ui.SettingsActivity;
import org.citra.citra_emu.features.settings.utils.SettingsFile;
import org.citra.citra_emu.model.GameDatabase;
import org.citra.citra_emu.model.GameProvider;
import org.citra.citra_emu.ui.platform.PlatformGamesFragment;
import org.citra.citra_emu.utils.AddDirectoryHelper;
import org.citra.citra_emu.utils.DirectoryInitialization;
import org.citra.citra_emu.utils.FileBrowserHelper;
import org.citra.citra_emu.utils.PermissionsHandler;
import org.citra.citra_emu.utils.PicassoUtils;
import org.citra.citra_emu.utils.StartupHandler;
import org.citra.citra_emu.utils.ThemeUtil;
import org.citra.citra_emu.utils.UpdaterUtils;

import java.util.Arrays;
import java.util.Collections;

/**
 * The main Activity of the Lollipop style UI. Manages several PlatformGamesFragments, which
 * individually display a grid of available games for each Fragment, in a tabbed layout.
 */
public final class MainActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private int mFrameLayoutId;
    private PlatformGamesFragment mPlatformGamesFragment;

    public static final int REQUEST_ADD_DIRECTORY = 1;
    public static final int REQUEST_INSTALL_CIA = 2;

    // Library
    private String mDirToAdd;
    private long mLastClickTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.applyTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();

        setSupportActionBar(mToolbar);

        mFrameLayoutId = R.id.games_platform_frame;
        String versionName = BuildConfig.VERSION_NAME;
        setVersionString(versionName);
        refreshGameList();

        if (savedInstanceState == null) {
            StartupHandler.HandleInit(this);
            if (PermissionsHandler.hasWriteAccess(this)) {
                mPlatformGamesFragment = new PlatformGamesFragment();
                getSupportFragmentManager().beginTransaction().add(mFrameLayoutId, mPlatformGamesFragment)
                        .commit();
            }
        } else {
            mPlatformGamesFragment = (PlatformGamesFragment) getSupportFragmentManager().getFragment(savedInstanceState, "mPlatformGamesFragment");
        }
        PicassoUtils.init();

        // Dismiss previous notifications (should not happen unless a crash occurred)
        EmulationActivity.tryDismissRunningNotification(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (PermissionsHandler.hasWriteAccess(this)) {
            if (getSupportFragmentManager() == null) {
                return;
            }
            if (outState == null) {
                return;
            }
            getSupportFragmentManager().putFragment(outState, "mPlatformGamesFragment", mPlatformGamesFragment);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        addDirIfNeeded(new AddDirectoryHelper(this));
    }

    // TODO: Replace with a ButterKnife injection.
    private void findViews() {
        mToolbar = findViewById(R.id.toolbar_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_game_grid, menu);

        return true;
    }

    public boolean handleOptionSelection(int itemId) {
        // Double-click prevention, using threshold of 500 ms
        if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
            return false;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        switch (itemId) {
            case R.id.menu_settings_core:
                launchSettingsActivity(SettingsFile.FILE_NAME_CONFIG);
                return true;

            case R.id.button_add_directory:
                launchFileListActivity(REQUEST_ADD_DIRECTORY);
                return true;

            case R.id.button_install_cia:
                launchFileListActivity(REQUEST_INSTALL_CIA);
                return true;

            case R.id.button_updater:
                openUpdaterDialog();
                return true;

            case R.id.button_credits:
                openCreditsDialog();
                return true;
        }

        return false;
    }

    public void setVersionString(String version) {
        mToolbar.setSubtitle("Gamer64_ytb");
    }

    public void refresh() {
        getContentResolver().insert(GameProvider.URI_REFRESH, null);
        refreshFragment();
    }

    public void launchSettingsActivity(String menuTag) {
        if (PermissionsHandler.hasWriteAccess(this)) {
            SettingsActivity.launch(this, menuTag, "");
        } else {
            PermissionsHandler.checkWritePermission(this);
        }
    }

    public void launchFileListActivity(int request) {
        if (PermissionsHandler.hasWriteAccess(this)) {
            switch (request) {
                case REQUEST_ADD_DIRECTORY:
                    FileBrowserHelper.openDirectoryPicker(this,
                            REQUEST_ADD_DIRECTORY,
                            R.string.select_game_folder,
                            Arrays.asList("elf", "axf", "cci", "3ds",
                                    "cxi", "app", "3dsx", "cia",
                                    "rar", "zip", "7z", "torrent",
                                    "tar", "gz"));
                    break;
                case REQUEST_INSTALL_CIA:
                    FileBrowserHelper.openFilePicker(this, REQUEST_INSTALL_CIA,
                            R.string.install_cia_title,
                            Collections.singletonList("cia"), true);
                    break;
            }
        } else {
            PermissionsHandler.checkWritePermission(this);
        }
    }

    public void openUpdaterDialog() {
        UpdaterUtils.openUpdaterWindow(this, null);
    }

    public void openCreditsDialog() {
        CreditsDialog dialogCredits = CreditsDialog.newInstance();
        dialogCredits.show(getSupportFragmentManager(), "CreditsDialog");
    }

    public void addDirIfNeeded(AddDirectoryHelper helper) {
        if (mDirToAdd != null) {
            helper.addDirectory(mDirToAdd, this::refresh);

            mDirToAdd = null;
        }
    }

    public void onDirectorySelected(String dir) {
        mDirToAdd = dir;
    }

    public void refreshGameList() {
        GameDatabase databaseHelper = CitraApplication.databaseHelper;
        databaseHelper.scanLibrary(databaseHelper.getWritableDatabase());
        refresh();
    }

    /**
     * @param requestCode An int describing whether the Activity that is returning did so successfully.
     * @param resultCode  An int describing what Activity is giving us this callback.
     * @param result      The information the returning Activity is providing us.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        switch (requestCode) {
            case REQUEST_ADD_DIRECTORY:
                // If the user picked a file, as opposed to just backing out.
                if (resultCode == MainActivity.RESULT_OK) {
                    // When a new directory is picked, we currently will reset the existing games
                    // database. This effectively means that only one game directory is supported.
                    // TODO(bunnei): Consider fixing this in the future, or removing code for this.
                    getContentResolver().insert(GameProvider.URI_RESET, null);
                    // Add the new directory
                    onDirectorySelected(FileBrowserHelper.getSelectedDirectory(result));
                }
                break;
            case REQUEST_INSTALL_CIA:
                // If the user picked a file, as opposed to just backing out.
                if (resultCode == MainActivity.RESULT_OK) {
                    NativeLibrary.InstallCIAS(FileBrowserHelper.getSelectedFiles(result));
                    refreshGameList();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionsHandler.REQUEST_CODE_WRITE_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    DirectoryInitialization.start(this);

                    mPlatformGamesFragment = new PlatformGamesFragment();
                    getSupportFragmentManager().beginTransaction().add(mFrameLayoutId, mPlatformGamesFragment)
                            .commit();

                    // Immediately prompt user to select a game directory on first boot
                    if (this != null) {
                        launchFileListActivity(REQUEST_ADD_DIRECTORY);
                    }
                } else {
                    Toast.makeText(this, R.string.write_permission_needed, Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    /**
     * Called by the framework whenever any actionbar/toolbar icon is clicked.
     *
     * @param item The icon that was clicked on.
     * @return True if the event was handled, false to bubble it up to the OS.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return handleOptionSelection(item.getItemId());
    }

    private void refreshFragment() {
        if (mPlatformGamesFragment != null) {
            mPlatformGamesFragment.refresh();
        }
    }

    @Override
    protected void onDestroy() {
        EmulationActivity.tryDismissRunningNotification(this);
        super.onDestroy();
    }
}
