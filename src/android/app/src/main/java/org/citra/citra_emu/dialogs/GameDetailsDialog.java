package org.citra.citra_emu.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.citra.citra_emu.NativeLibrary;
import org.citra.citra_emu.R;
import org.citra.citra_emu.activities.EditorActivity;
import org.citra.citra_emu.activities.EmulationActivity;

import java.nio.IntBuffer;

public final class GameDetailsDialog extends DialogFragment
{
    private static final String ARG_GAME_PATH = "game_path";
    private Bitmap mIcon;
    private static String mGamePath;

    public static GameDetailsDialog newInstance(String gamePath)
    {
        GameDetailsDialog fragment = new GameDetailsDialog();
        mGamePath = gamePath;

        Bundle arguments = new Bundle();
        arguments.putString(ARG_GAME_PATH, gamePath);
        fragment.setArguments(arguments);

        return fragment;
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
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ViewGroup contents = (ViewGroup) getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_game_details, null);

        // game title
        TextView textGameTitle = contents.findViewById(R.id.text_game_title);
        textGameTitle.setText(NativeLibrary.GetTitle(mGamePath));

        // game id
        TextView textGameId = contents.findViewById(R.id.text_game_id);
        textGameId.setText("ID: " + NativeLibrary.GetAppId(mGamePath));

        Button buttonLaunchGame = contents.findViewById(R.id.button_launch_game);
        buttonLaunchGame.setOnClickListener(view ->
        {
            this.dismiss();
            EmulationActivity.launch((FragmentActivity) view.getContext(), mGamePath, NativeLibrary.GetTitle(mGamePath));
        });

        Button buttonCheatCode = contents.findViewById(R.id.button_cheat_code);
        buttonCheatCode.setOnClickListener(view ->
        {
            this.dismiss();
            EditorActivity.launch(getContext(), NativeLibrary.GetAppId(mGamePath), NativeLibrary.GetTitle(mGamePath));
        });

        FragmentManager fm = ((FragmentActivity) contents.getContext()).getSupportFragmentManager();
        ShortcutDialog shortcutDialog = ShortcutDialog.newInstance(mGamePath);
        Button buttonShortcut = contents.findViewById(R.id.button_shortcut);
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(getContext())) {
            buttonShortcut.setVisibility(View.VISIBLE);
        } else {
            buttonShortcut.setVisibility(View.INVISIBLE);
        }
        buttonShortcut.setOnClickListener(view ->
        {
            this.dismiss();
            shortcutDialog.show(fm, "fragment_manager");
        });

        ImageView imageGameScreen = contents.findViewById(R.id.image_game_screen);
        imageGameScreen.setImageBitmap(getIcon(getContext()));

        builder.setView(contents);
        return builder.create();
    }
}