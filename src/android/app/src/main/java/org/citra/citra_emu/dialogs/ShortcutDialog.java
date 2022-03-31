package org.citra.citra_emu.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.DialogFragment;

import org.citra.citra_emu.NativeLibrary;
import org.citra.citra_emu.activities.EmulationActivity;
import org.citra.citra_emu.R;

import java.nio.IntBuffer;

public final class ShortcutDialog extends DialogFragment {
    public static final String EXTRA_GAME_PATH = "GamePath";
    private String mGamePath;
    private EditText mName;
    private Bitmap mIcon;

    public static ShortcutDialog newInstance(String path) {
        return new ShortcutDialog(path);
    }

    public ShortcutDialog(String path) {
        mGamePath = path;
    }

    public Bitmap getIcon(Context context) {
        if (mIcon == null) {
            int[] pixels = NativeLibrary.GetIcon(mGamePath);
            if (pixels == null || pixels.length == 0) {
                mIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.no_icon);
            } else {
                Bitmap icon = Bitmap.createBitmap(48, 48, Bitmap.Config.RGB_565);
                icon.copyPixelsFromBuffer(IntBuffer.wrap(pixels));
                mIcon = roundBitmap(resizeBitmap(icon, 96, 96), 10);
            }
        }
        return mIcon;
    }

    public static Bitmap roundBitmap(Bitmap icon, float radius) {
        Rect rect = new Rect(0, 0, icon.getWidth(), icon.getHeight());
        Bitmap output =
                Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        BitmapShader shader =
                new BitmapShader(icon, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);
        // rect contains the bounds of the shape
        // radius is the radius in pixels of the rounded corners
        // paint contains the shader that will texture the shape
        canvas.drawRoundRect(new RectF(rect), radius, radius, paint);
        return output;
    }

    public static Bitmap resizeBitmap(Bitmap icon, int newWidth, int newHeight) {
        int width = icon.getWidth();
        int height = icon.getHeight();
        float scaleWidth = newWidth / (float) width;
        float scaleHeight = newHeight / (float) height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(icon, 0, 0, width, height, matrix, true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (savedInstanceState != null) {
            mGamePath = savedInstanceState.getString(EXTRA_GAME_PATH);
        }

        ViewGroup contents =
                (ViewGroup)getActivity().getLayoutInflater().inflate(R.layout.dialog_shortcut, null);

        ImageView imageIcon = contents.findViewById(R.id.image_game_screen);
        imageIcon.setImageBitmap(getIcon(getContext()));

        mName = contents.findViewById(R.id.text_name);
        mName.setText(NativeLibrary.GetTitle(mGamePath));

        Button buttonPermission = contents.findViewById(R.id.button_permission);
        buttonPermission.setOnClickListener(view -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        Button buttonConfirm = contents.findViewById(R.id.button_confirm);
        buttonConfirm.setOnClickListener(view -> addShortcut());

        builder.setView(contents);
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(EXTRA_GAME_PATH, mGamePath);
        super.onSaveInstanceState(outState);
    }

    private void addShortcut() {
        final String id = NativeLibrary.GetAppId(mGamePath);
        final String name = mName.getText().toString();
        final Bitmap icon = getIcon(getContext());
        final Intent intent = new Intent(getContext(), EmulationActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(NativeLibrary.GetAppId(mGamePath), id);
        intent.putExtra(EmulationActivity.EXTRA_SELECTED_TITLE, name);
        intent.putExtra(EmulationActivity.EXTRA_SELECTED_GAME, mGamePath);
        ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(getContext(), id)
                .setShortLabel(name)
                .setIcon(IconCompat.createWithBitmap(icon))
                .setIntent(intent)
                .build();
        ShortcutManagerCompat.requestPinShortcut(getContext(), shortcutInfo, null);
        dismiss();
    }
}