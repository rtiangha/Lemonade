package org.citra.citra_emu.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.nononsenseapps.filepicker.AbstractFilePickerFragment;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.FilePickerFragment;

import org.citra.citra_emu.R;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CustomFilePickerActivity extends FilePickerActivity implements DialogInterface.OnClickListener {
    public static final String EXTRA_TITLE = "filepicker.intent.TITLE";
    public static final String EXTRA_EXTENSIONS = "filepicker.intent.EXTENSIONS";
    private CustomFilePickerFragment mFilePickerFragment;

    @Override
    protected AbstractFilePickerFragment<File> getFragment(
            @Nullable final String startPath, final int mode, final boolean allowMultiple,
            final boolean allowCreateDir, final boolean allowExistingFile,
            final boolean singleClick) {
        mFilePickerFragment = new CustomFilePickerFragment();
        // startPath is allowed to be null. In that case, default folder should be SD-card and not "/"
        mFilePickerFragment.setArgs(
                startPath != null ? startPath : Environment.getExternalStorageDirectory().getPath(),
                mode, allowMultiple, allowCreateDir, allowExistingFile, singleClick);

        Intent intent = getIntent();
        int title = intent == null ? 0 : intent.getIntExtra(EXTRA_TITLE, 0);
        mFilePickerFragment.setTitle(title);
        String allowedExtensions = intent == null ? "*" : intent.getStringExtra(EXTRA_EXTENSIONS);
        mFilePickerFragment.setAllowedExtensions(allowedExtensions);

        return mFilePickerFragment;
    }

    public static class CustomFilePickerFragment extends FilePickerFragment {
        private static String ALL_FILES = "*";
        private int mTitle;
        private static List<String> extensions = Collections.singletonList(ALL_FILES);

        @NonNull
        @Override
        public Uri toUri(@NonNull final File file) {
            return FileProvider
                    .getUriForFile(getContext(),
                            getContext().getApplicationContext().getPackageName() + ".filesprovider",
                            file);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            if (mode == MODE_DIR) {
                TextView ok = getActivity().findViewById(R.id.nnf_button_ok);
                ok.setText(R.string.select_dir);

                TextView cancel = getActivity().findViewById(R.id.nnf_button_cancel);
                cancel.setVisibility(View.GONE);
            }
        }

        @Override
        protected View inflateRootView(LayoutInflater inflater, ViewGroup container) {
            View view = super.inflateRootView(inflater, container);
            if (mTitle != 0) {
                Toolbar toolbar = view.findViewById(com.nononsenseapps.filepicker.R.id.nnf_picker_toolbar);
                ViewGroup parent = (ViewGroup) toolbar.getParent();
                int index = parent.indexOfChild(toolbar);
                View newToolbar = inflater.inflate(R.layout.filepicker_toolbar, toolbar, false);
                TextView title = newToolbar.findViewById(R.id.filepicker_title);
                title.setText(mTitle);
                parent.removeView(toolbar);
                parent.addView(newToolbar, index);
            }
            return view;
        }

        public void setTitle(int title) {
            mTitle = title;
        }

        public void setAllowedExtensions(String allowedExtensions) {
            if (allowedExtensions == null)
                return;

            extensions = Arrays.asList(allowedExtensions.split(","));
        }

        @Override
        protected boolean isItemVisible(@NonNull final File file) {
            // Some users jump to the conclusion that Dolphin isn't able to detect their
            // files if the files don't show up in the file picker when mode == MODE_DIR.
            // To avoid this, show files even when the user needs to select a directory.
            return (showHiddenItems || !file.isHidden()) &&
                    (file.isDirectory() || extensions.contains(ALL_FILES) ||
                            extensions.contains(fileExtension(file.getName()).toLowerCase()));
        }

        @Override
        public boolean isCheckable(@NonNull final File file) {
            // We need to make a small correction to the isCheckable logic due to
            // overriding isItemVisible to show files when mode == MODE_DIR.
            // AbstractFilePickerFragment always treats files as checkable when
            // allowExistingFile == true, but we don't want files to be checkable when mode == MODE_DIR.
            return super.isCheckable(file) && !(mode == MODE_DIR && file.isFile());
        }

        @Override
        public void goUp() {
            if (Environment.getExternalStorageDirectory().getPath().equals(mCurrentPath.getPath())) {
                goToDir(new File("/storage/"));
                return;
            }
            if (mCurrentPath.equals(new File("/storage/"))) {
                return;
            }
            super.goUp();
        }

        @Override
        public void onClickDir(@NonNull View view, @NonNull DirViewHolder viewHolder) {
            if (viewHolder.file.equals(new File("/storage/emulated/")))
                viewHolder.file = new File("/storage/emulated/0/");
            super.onClickDir(view, viewHolder);
        }

        private static String fileExtension(@NonNull String filename) {
            int i = filename.lastIndexOf('.');
            return i < 0 ? "" : filename.substring(i + 1);
        }


        public String getCurrentPath() {
            return mCurrentPath.toString();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_file_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_current_directory:
                showEditorDialog();
                return true;
        }

        return false;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        EditText editor = ((AlertDialog) dialog).findViewById(R.id.setting_editor);
        String text = editor.getText().toString();
        File path = new File(text);
        if (path.isDirectory()) {
            mFilePickerFragment.goToDir(path);
        } else {
            Toast.makeText(this, R.string.nnf_need_valid_filename, Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_editor, null);

        builder.setTitle(R.string.current_directory);
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.show();

        EditText editor = view.findViewById(R.id.setting_editor);
        editor.setText(mFilePickerFragment.getCurrentPath());
        editor.requestFocus();
    }
}
