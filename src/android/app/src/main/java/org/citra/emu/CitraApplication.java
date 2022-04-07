// Copyright 2019 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package org.citra.emu;

import android.app.Application;
import android.content.Context;

import org.citra.emu.model.GameDatabase;
import org.citra.emu.utils.DirectoryInitialization;
import org.citra.emu.utils.PermissionsHandler;
import org.citra.emu.utils.VolleyUtil;

public class CitraApplication extends Application {
    public static GameDatabase databaseHelper;
    private static CitraApplication application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        VolleyUtil.init(getApplicationContext());

        if (PermissionsHandler.hasWriteAccess(getApplicationContext())) {
            DirectoryInitialization.start(getApplicationContext());
        }

        databaseHelper = new GameDatabase(this);
    }

    public static Context getAppContext() {
        return application.getApplicationContext();
    }
}
