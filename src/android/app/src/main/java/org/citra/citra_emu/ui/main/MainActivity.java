package org.citra.citra_emu.ui.main;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.citra.citra_emu.CitraApplication;
import org.citra.citra_emu.NativeLibrary;
import org.citra.citra_emu.R;
import org.citra.citra_emu.activities.EditorActivity;
import org.citra.citra_emu.activities.EmulationActivity;
import org.citra.citra_emu.features.settings.ui.SettingsActivity;
import org.citra.citra_emu.features.settings.utils.SettingsFile;
import org.citra.citra_emu.model.GameDatabase;
import org.citra.citra_emu.model.GameProvider;
import org.citra.citra_emu.ui.DividerItemDecoration;
import org.citra.citra_emu.ui.platform.PlatformGamesFragment;
import org.citra.citra_emu.utils.AddDirectoryHelper;
import org.citra.citra_emu.utils.DirectoryInitialization;
import org.citra.citra_emu.utils.FileBrowserHelper;
import org.citra.citra_emu.utils.Log;
import org.citra.citra_emu.utils.PermissionsHandler;
import org.citra.citra_emu.utils.PicassoUtils;
import org.citra.citra_emu.utils.StartupHandler;
import org.citra.citra_emu.utils.ThemeUtil;
import org.citra.citra_emu.utils.UpdaterUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * The MainActivity of the Lollipop style UI. Manages several PlatformGamesFragments, which
 * individually display a grid of available games for each Fragment, in a tabbed layout.
 */
public final class MainActivity extends AppCompatActivity {
    public static final int REQUEST_ADD_DIRECTORY = 1;
    public static final int REQUEST_INSTALL_CIA = 2;

    private String mDirToAdd;
    private long mLastClickTime = 0;
    private Toolbar mToolbar;
    private int mFrameLayoutId;
    private PlatformGamesFragment mPlatformGamesFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.applyTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();

        setSupportActionBar(mToolbar);

        mFrameLayoutId = R.id.games_platform_frame;
        refreshGameList();

        mToolbar.setOnMenuItemClickListener(menuItem -> {
            // Double-click prevention, using threshold of 500 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
                return false;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            switch (menuItem.getItemId()) {
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
            }
            return false;
        });

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

    private void refreshFragment() {
        if (mPlatformGamesFragment != null) {
            mPlatformGamesFragment.refresh();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * A simple class that stores references to views so that the GameAdapter doesn't need to
     * keep calling findViewById(), which is expensive.
     */
    public static class GameViewHolder extends RecyclerView.ViewHolder {
        private View itemView;
        public ImageView imageIcon;
        public TextView textGameTitle;
        public TextView textCompany;
        public TextView textFileName;

        public String gameId;

        // TODO Not need any of this stuff. Currently only the properties dialog needs it.
        public String path;
        public String title;
        public String description;
        public String regions;
        public String company;

        public GameViewHolder(View itemView) {
            super(itemView);

            this.itemView = itemView;
            itemView.setTag(this);

            imageIcon = itemView.findViewById(R.id.image_game_screen);
            textGameTitle = itemView.findViewById(R.id.text_game_title);
            textCompany = itemView.findViewById(R.id.text_company);
            textFileName = itemView.findViewById(R.id.text_filename);
        }

        public View getItemView() {
            return itemView;
        }
    }

    /**
     * This adapter gets its information from a database Cursor. This fact, paired with the usage of
     * ContentProviders and Loaders, allows for efficient display of a limited view into a (possibly)
     * large dataset.
     */
    public static final class GameAdapter extends RecyclerView.Adapter<GameViewHolder> implements
            View.OnClickListener, View.OnLongClickListener {
        private Cursor mCursor;
        private GameDataSetObserver mObserver;

        private boolean mDatasetValid;
        private long mLastClickTime = 0;

        /**
         * Initializes the adapter's observer, which watches for changes to the dataset. The adapter will
         * display no data until a Cursor is supplied by a CursorLoader.
         */
        public GameAdapter() {
            mDatasetValid = false;
            mObserver = new GameDataSetObserver();
        }

        /**
         * Called by the LayoutManager when it is necessary to create a new view.
         *
         * @param parent   The RecyclerView (I think?) the created view will be thrown into.
         * @param viewType Not used here, but useful when more than one type of child will be used in the RecyclerView.
         * @return The created ViewHolder with references to all the child view's members.
         */
        @Override
        public GameViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Create a new view.
            View gameCard = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_game, parent, false);

            gameCard.setOnClickListener(this);
            gameCard.setOnLongClickListener(this);

            // Use that view to create a ViewHolder.
            return new GameViewHolder(gameCard);
        }

        /**
         * Called by the LayoutManager when a new view is not necessary because we can recycle
         * an existing one (for example, if a view just scrolled onto the screen from the bottom, we
         * can use the view that just scrolled off the top instead of inflating a new one.)
         *
         * @param holder   A ViewHolder representing the view we're recycling.
         * @param position The position of the 'new' view in the dataset.
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
            if (mDatasetValid) {
                if (mCursor.moveToPosition(position)) {
                    PicassoUtils.loadGameIcon(holder.imageIcon,
                            mCursor.getString(GameDatabase.GAME_COLUMN_PATH));

                    holder.textGameTitle.setText(mCursor.getString(GameDatabase.GAME_COLUMN_TITLE).replaceAll("[\\t\\n\\r]+", " "));
                    holder.textCompany.setText(mCursor.getString(GameDatabase.GAME_COLUMN_COMPANY));

                    final Path gamePath = Paths.get(mCursor.getString(GameDatabase.GAME_COLUMN_PATH));
                    holder.textFileName.setText(gamePath.getFileName().toString());

                    // TODO These shouldn't be necessary once the move to a DB-based model is complete.
                    holder.gameId = mCursor.getString(GameDatabase.GAME_COLUMN_GAME_ID);
                    holder.path = mCursor.getString(GameDatabase.GAME_COLUMN_PATH);
                    holder.title = mCursor.getString(GameDatabase.GAME_COLUMN_TITLE);
                    holder.description = mCursor.getString(GameDatabase.GAME_COLUMN_DESCRIPTION);
                    holder.regions = mCursor.getString(GameDatabase.GAME_COLUMN_REGIONS);
                    holder.company = mCursor.getString(GameDatabase.GAME_COLUMN_COMPANY);

                    final int backgroundColorId = isValidGame(holder.path) ? R.color.card_view_background : R.color.card_view_disabled;
                    View itemView = holder.getItemView();
                    itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), backgroundColorId));
                } else {
                    Log.error("[GameAdapter] Can't bind view; Cursor is not valid.");
                }
            } else {
                Log.error("[GameAdapter] Can't bind view; dataset is not valid.");
            }
        }

        /**
         * Called by the LayoutManager to find out how much data we have.
         *
         * @return Size of the dataset.
         */
        @Override
        public int getItemCount() {
            if (mDatasetValid && mCursor != null) {
                return mCursor.getCount();
            }
            Log.error("[GameAdapter] Dataset is not valid.");
            return 0;
        }

        /**
         * Return the contents of the _id column for a given row.
         *
         * @param position The row for which Android wants an ID.
         * @return A valid ID from the database, or 0 if not available.
         */
        @Override
        public long getItemId(int position) {
            if (mDatasetValid && mCursor != null) {
                if (mCursor.moveToPosition(position)) {
                    return mCursor.getLong(GameDatabase.COLUMN_DB_ID);
                }
            }

            Log.error("[GameAdapter] Dataset is not valid.");
            return 0;
        }

        /**
         * Tell Android whether or not each item in the dataset has a stable identifier.
         * Which it does, because it's a database, so always tell Android 'true'.
         *
         * @param hasStableIds ignored.
         */
        @Override
        public void setHasStableIds(boolean hasStableIds) {
            super.setHasStableIds(true);
        }

        /**
         * When a load is finished, call this to replace the existing data with the newly-loaded
         * data.
         *
         * @param cursor The newly-loaded Cursor.
         */
        public void swapCursor(Cursor cursor) {
            // Sanity check.
            if (cursor == mCursor) {
                return;
            }

            // Before getting rid of the old cursor, disassociate it from the Observer.
            final Cursor oldCursor = mCursor;
            if (oldCursor != null && mObserver != null) {
                oldCursor.unregisterDataSetObserver(mObserver);
            }

            mCursor = cursor;
            if (mCursor != null) {
                // Attempt to associate the new Cursor with the Observer.
                if (mObserver != null) {
                    mCursor.registerDataSetObserver(mObserver);
                }

                mDatasetValid = true;
            } else {
                mDatasetValid = false;
            }

            notifyDataSetChanged();
        }

        /**
         * Launches the game that was clicked on.
         *
         * @param view The card representing the game the user wants to play.
         */
        @Override
        public void onClick(View view) {
            // Double-click prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            GameViewHolder holder = (GameViewHolder) view.getTag();

            EmulationActivity.launch((FragmentActivity) view.getContext(), holder.path, holder.title);
        }

        /**
         * Show some game details.
         *
         * @param clicked Display the AlertDialog with game info.
         */
        @Override
        public boolean onLongClick(View clicked) {
            GameViewHolder holder = (GameViewHolder) clicked.getTag();
            String gameId = NativeLibrary.GetAppId(holder.path);
            EditorActivity.launch(clicked.getContext(), gameId, holder.title);
            return true;
        }

        public static class SpacesItemDecoration extends DividerItemDecoration {
            private int space;

            public SpacesItemDecoration(Drawable divider, int space) {
                super(divider);
                this.space = space;
            }

            @Override
            public void getItemOffsets(Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                outRect.left = 0;
                outRect.right = 0;
                outRect.bottom = space;
                outRect.top = 0;
            }
        }

        private boolean isValidGame(String path) {
            return Stream.of(
                    ".rar", ".zip", ".7z", ".torrent", ".tar", ".gz").noneMatch(suffix -> path.toLowerCase().endsWith(suffix));
        }

        private final class GameDataSetObserver extends DataSetObserver {
            @Override
            public void onChanged() {
                super.onChanged();

                mDatasetValid = true;
                notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();

                mDatasetValid = false;
                notifyDataSetChanged();
            }
        }
    }
}
