package org.citra.citra_emu.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.nononsenseapps.filepicker.DividerItemDecoration;

import org.citra.citra_emu.NativeLibrary;
import org.citra.citra_emu.R;
import org.citra.citra_emu.overlay.InputOverlay;

import java.util.ArrayList;

public class RunningSettingDialog extends DialogFragment {
    private SettingsAdapter mAdapter;
    private TextView mInfo;
    private Handler mHandler;

    public static RunningSettingDialog newInstance() {
        return new RunningSettingDialog();
    }

    public void setHeapInfo() {
        long heapsize = Debug.getNativeHeapAllocatedSize() >> 20;
        mInfo.setText(String.format("RAM:%dMB", heapsize));
        mHandler.postDelayed(this::setHeapInfo, 1000);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ViewGroup contents = (ViewGroup) getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_running_settings, null);

        mInfo = contents.findViewById(R.id.text_info);
        mHandler = new Handler(getActivity().getMainLooper());
        setHeapInfo();

        //int columns = 1;
        Drawable lineDivider = getContext().getDrawable(R.drawable.gamelist_divider);
        RecyclerView recyclerView = contents.findViewById(R.id.list_settings);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new SettingsAdapter();
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(lineDivider));
        builder.setView(contents);
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mAdapter.saveSettings();
    }

    public class SettingsItem {
        // setting type
        public static final int SETTING_FMV_HACK = 0;
        public static final int SETTING_SHOW_FPS = 1;
        public static final int SETTING_LINEAR_FILTER = 2;
        public static final int SETTING_AUDIO_STRECHING = 3;
        public static final int SETTING_SPEED_LIMIT = 4;

        // view type
        public static final int TYPE_CHECKBOX = 0;
        public static final int TYPE_SEEK_BAR = 2;

        private int mSetting;
        private String mName;
        private int mType;
        private int mValue;

        public SettingsItem(int setting, int nameId, int type, int value) {
            mSetting = setting;
            mName = getString(nameId);
            mType = type;
            mValue = value;
        }

        public SettingsItem(int setting, String name, int type, int value) {
            mSetting = setting;
            mName = name;
            mType = type;
            mValue = value;
        }

        public int getType() {
            return mType;
        }

        public int getSetting() {
            return mSetting;
        }

        public String getName() {
            return mName;
        }

        public int getValue() {
            return mValue;
        }

        public void setValue(int value) {
            mValue = value;
        }
    }

    public abstract class SettingViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public SettingViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            findViews(itemView);
        }

        protected abstract void findViews(View root);

        public abstract void bind(SettingsItem item);

        public abstract void onClick(View clicked);
    }

    public final class CheckBoxSettingViewHolder extends SettingViewHolder
            implements CompoundButton.OnCheckedChangeListener {
        SettingsItem mItem;
        private TextView mTextSettingName;
        private Switch mSwitch;

        public CheckBoxSettingViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        protected void findViews(View root) {
            mTextSettingName = root.findViewById(R.id.text_setting_name);
            mSwitch = root.findViewById(R.id.checkbox);
            mSwitch.setOnCheckedChangeListener(this);
        }

        @Override
        public void bind(SettingsItem item) {
            mItem = item;
            mTextSettingName.setText(item.getName());
            mSwitch.setChecked(mItem.getValue() > 0);
        }

        @Override
        public void onClick(View clicked) {
            mSwitch.toggle();
            mItem.setValue(mSwitch.isChecked() ? 1 : 0);
        }

        @Override
        public void onCheckedChanged(CompoundButton view, boolean isChecked) {
            mItem.setValue(isChecked ? 1 : 0);
        }
    }

    public final class SeekBarSettingViewHolder extends SettingViewHolder {
        SettingsItem mItem;
        private TextView mTextSettingName;
        private TextView mTextSettingValue;
        private SeekBar mSeekBar;

        public SeekBarSettingViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        protected void findViews(View root) {
            mTextSettingName = root.findViewById(R.id.text_setting_name);
            mTextSettingValue = root.findViewById(R.id.text_setting_value);
            mSeekBar = root.findViewById(R.id.seekbar);
            mSeekBar.setProgress(99);
        }

        @Override
        public void bind(SettingsItem item) {
            mItem = item;
            mTextSettingName.setText(item.getName());
            mSeekBar.setMax(100);
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                    if (seekBar.getMax() > 99) {
                        progress = (progress / 5) * 5;
                        mTextSettingValue.setText(progress + "%");
                    } else {
                        mTextSettingValue.setText(String.valueOf(progress));
                    }
                    mItem.setValue(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            mSeekBar.setProgress(item.getValue());
        }

        @Override
        public void onClick(View clicked) {

        }
    }

    public class SettingsAdapter extends RecyclerView.Adapter<SettingViewHolder> {
        private int[] mRunningSettings;
        //private int mJoystickRelative;
        private ArrayList<SettingsItem> mSettings;

        public SettingsAdapter() {
            int i = 0;
            //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            mRunningSettings = NativeLibrary.getRunningSettings();
            mSettings = new ArrayList<>();

            // native settings
            mSettings.add(new SettingsItem(SettingsItem.SETTING_FMV_HACK,
                    R.string.fmv_hack, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_SHOW_FPS,
                    R.string.emulation_show_fps, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_LINEAR_FILTER,
                    R.string.linear_filtering, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_AUDIO_STRECHING,
                    R.string.audio_stretch, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_SPEED_LIMIT,
                    R.string.frame_limit_slider, SettingsItem.TYPE_SEEK_BAR,
                    mRunningSettings[i++]));
        }

        @NonNull
        @Override
        public SettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView;
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case SettingsItem.TYPE_CHECKBOX:
                    itemView = inflater.inflate(R.layout.list_item_running_checkbox, parent, false);
                    return new CheckBoxSettingViewHolder(itemView);
                case SettingsItem.TYPE_SEEK_BAR:
                    itemView = inflater.inflate(R.layout.list_item_running_seekbar, parent, false);
                    return new SeekBarSettingViewHolder(itemView);
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return mSettings.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mSettings.get(position).getType();
        }

        @Override
        public void onBindViewHolder(@NonNull SettingViewHolder holder, int position) {
            holder.bind(mSettings.get(position));
        }

        public void saveSettings() {
            // native settings
            boolean isChanged = false;
            int[] newSettings = new int[mRunningSettings.length];
            for (int i = 0; i < mRunningSettings.length; ++i) {
                newSettings[i] = mSettings.get(i).getValue();
                if (newSettings[i] != mRunningSettings[i]) {
                    isChanged = true;
                }
            }
            if (isChanged) {
                NativeLibrary.setRunningSettings(newSettings);
            }
        }
    }

}