package org.citra.citra_emu.utils;

import java.io.File;

import android.util.Log;
import android.content.Context;
import android.os.Environment;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.citra.citra_emu.BuildConfig;
import org.citra.citra_emu.R;
import org.citra.citra_emu.model.UpdaterData;
import org.citra.citra_emu.dialogs.UpdaterDialog;

public class UpdaterUtils {
    public static final String URL = "https://api.github.com/repos/Gamer64ytb/Citra-Enhanced/releases";
    public static final String URL_LATEST = "https://api.github.com/repos/Gamer64ytb/Citra-Enhanced/releases/latest";

    public static void openUpdaterWindow(Context context, UpdaterData data) {
        FragmentManager fm = ((AppCompatActivity) context).getSupportFragmentManager();
        UpdaterDialog updaterDialog = UpdaterDialog.newInstance(data);
        updaterDialog.show(fm, "fragment_updater");
    }

    public static void checkUpdatesInit(Context context) {
       cleanDownloadFolder(context);
       checkUpdates(context);
    }

    private static void checkUpdates(Context context) {
        makeDataRequest(new LoadCallback<UpdaterData>() {
            @Override
            public void onLoad(UpdaterData data) {
               showUpdateMessage(context, data);
            }

            @Override
            public void onLoadError() {}
        });
    }

    private static void showUpdateMessage(Context context, UpdaterData data) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.updates_alert))
                .setMessage(context.getString(R.string.updater_alert_body))
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) ->
                        openUpdaterWindow(context, data))
                .setNeutralButton(R.string.not_now,
                        ((dialogInterface, i) -> dialogInterface.dismiss()))
                .show();
    }

    public static void makeDataRequest(LoadCallback<UpdaterData> listener) {
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, URL_LATEST, null,
                response -> {
                    try {
                        UpdaterData data = new UpdaterData(response);
                        listener.onLoad(data);
                    } catch (Exception e) {
                        listener.onLoadError();
                    }
                },
                error -> listener.onLoadError());
        VolleyUtil.getQueue().add(jsonRequest);
    }

    public static void makeChangelogRequest(String format, LoadCallback<String> listener) {
        JsonArrayRequest jsonRequest = new JsonArrayRequest(Request.Method.GET, URL, null,
                response -> {
                    try {
                        StringBuilder changelog = new StringBuilder();

                        for (int i = 0; i < response.length(); i++) {
                            changelog.append(String.format(format,
                                    response.getJSONObject(i).getInt("id"),
                                    response.getJSONObject(i).getString("published_at").substring(0, 10),
                                    response.getJSONObject(i).getString("body")));
                        }
                        changelog.setLength(Math.max(changelog.length() - 1, 0));
                        listener.onLoad(changelog.toString());
                    } catch (Exception e) {
                        listener.onLoadError();
                    }
                },
                error -> listener.onLoadError());
        VolleyUtil.getQueue().add(jsonRequest);
    }

    public static void cleanDownloadFolder(Context context) {
        File downloadFolder = getDownloadFolder(context);
        for (File file : downloadFolder.listFiles())
            file.delete();
    }

    public static File getDownloadFolder(Context context) {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
    }

    public static int getBuildVersion() {
        try {
            return BuildConfig.VERSION_CODE;
        } catch (Exception e) {
            return -1;
        }
    }
}