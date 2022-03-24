package org.citra.citra_emu.ui.main;

/**
 * Abstraction for the screen that shows on application launch.
 * Implementations will differ primarily to target touch-screen
 * or non-touch screen devices.
 */
public interface MainView {
    /**
     * Tell the view to refresh its contents.
     */
    void refresh();

    void launchSettingsActivity(String menuTag);

    void launchFileListActivity(int request);

    void openUpdaterDialog();

    void openCreditsDialog();
}
