// Copyright 2019 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

package org.citra.citra_emu;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import org.citra.citra_emu.model.GameDatabase;
import org.citra.citra_emu.utils.DirectoryInitialization;
import org.citra.citra_emu.utils.PermissionsHandler;
import org.citra.citra_emu.utils.VolleyUtil;

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
