package org.citra.citra_emu.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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

import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.nononsenseapps.filepicker.DividerItemDecoration;

import org.citra.citra_emu.NativeLibrary;
import org.citra.citra_emu.R;
import org.citra.citra_emu.activities.EmulationActivity;
import org.citra.citra_emu.overlay.InputOverlay;
import org.citra.citra_emu.utils.EmulationMenuSettings;

import java.util.ArrayList;

public class RunningSettingDialog extends DialogFragment {
    public static final int MENU_MAIN = 0;
    public static final int MENU_SETTINGS = 1;

    private int mMenu;
    private TextView mTitle;
    private SettingsAdapter mAdapter;
    private TextView mInfo;
    private Handler mHandler;
    private DialogInterface.OnDismissListener mDismissListener;

    public static RunningSettingDialog newInstance() {
        return new RunningSettingDialog();
    }

    public void setHeapInfo() {
        long heapsize = Debug.getNativeHeapAllocatedSize() >> 20;
        mInfo.setText(String.format("RAM:%dMB", heapsize));
        mHandler.postDelayed(this::setHeapInfo, 1000);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Update framebuffer layout when closing the settings
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        NativeLibrary.NotifyOrientationChange(EmulationMenuSettings.getLandscapeScreenLayout(),
                rotation);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ViewGroup contents = (ViewGroup) getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_running_settings, null);

        mTitle = contents.findViewById(R.id.text_title);
        mInfo = contents.findViewById(R.id.text_info);
        mHandler = new Handler(getActivity().getMainLooper());
        setHeapInfo();

        Drawable lineDivider = getContext().getDrawable(R.drawable.gamelist_divider);
        RecyclerView recyclerView = contents.findViewById(R.id.list_settings);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new SettingsAdapter();
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(lineDivider));
        builder.setView(contents);
        loadSubMenu(MENU_MAIN);
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mMenu == MENU_SETTINGS) {
            mAdapter.saveSettings();
        }
        if (mDismissListener != null) {
            mDismissListener.onDismiss(dialog);
        }
        mHandler.removeCallbacksAndMessages(null);
    }

    private void loadSubMenu(int menu) {
        if (menu == MENU_MAIN) {
            EmulationActivity activity = (EmulationActivity) NativeLibrary.getEmulationContext();
            mTitle.setText(activity.getGameTitle());
            mAdapter.loadMainMenu();
        } else if (menu == MENU_SETTINGS) {
            mTitle.setText(R.string.preferences_settings);
            mAdapter.loadSettingsMenu();
        }
        mMenu = menu;
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        mDismissListener = listener;
    }

    public class SettingsItem {
        // setting type
        public static final int SETTING_FMV_HACK = 0;
        public static final int SETTING_SHOW_FPS = 1;
        public static final int SETTING_SCALE_FACTOR = 2;
        public static final int SETTING_LINEAR_FILTER = 4;
        public static final int SETTING_SKIP_SLOW_DRAW = 5;
        public static final int SETTING_SKIP_CPU_WRITE = 6;
        public static final int SETTING_SKIP_TEXTURE_COPY = 7;
        public static final int SETTING_SKIP_FORMAT_REINTERPRETATION = 8;
        public static final int SETTING_TEXTURE_LOAD_HACK = 9;
        public static final int SETTING_ACCURATE_MUL = 10;
        public static final int SETTING_CUSTOM_LAYOUT = 11;
        public static final int SETTING_SPEED_LIMIT = 12;

        // pref
        public static final int SETTING_HAPTIC_FEEDBACK = 100;
        public static final int SETTING_JOYSTICK_RELATIVE = 101;
        public static final int SETTING_SHOW_OVERLAY = 102;
        public static final int SETTING_CONTROLLER_SCALE = 103;
        public static final int SETTING_CONTROLLER_ALPHA = 104;
        public static final int SETTING_SCREEN_LAYOUT = 105;

        // func
        public static final int SETTING_LOAD_SUBMENU = 200;
        public static final int SETTING_EDIT_BUTTONS = 201;
        public static final int SETTING_SAVE_STATE = 202;
        public static final int SETTING_LOAD_STATE = 203;
        public static final int SETTING_LOAD_AMIIBO = 204;
        public static final int SETTING_REMOVE_AMIIBO = 205;
        public static final int SETTING_TOGGLE_CONTROLS = 206;
        public static final int SETTING_RESET_OVERLAY = 207;
        public static final int SETTING_ROTATE_SCREEN = 208;
        public static final int SETTING_CHEAT_CODE = 209;
        public static final int SETTING_EXIT_GAME = 210;

        // view type
        public static final int TYPE_CHECKBOX = 0;
        public static final int TYPE_RADIO_GROUP = 1;
        public static final int TYPE_SEEK_BAR = 2;
        public static final int TYPE_BUTTON = 3;

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

    public final class ButtonSettingViewHolder extends SettingViewHolder {
        SettingsItem mItem;
        private TextView mName;

        public ButtonSettingViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        protected void findViews(View root) {
            mName = root.findViewById(R.id.text_setting_name);
        }

        @Override
        public void bind(SettingsItem item) {
            mItem = item;
            mName.setText(item.getName());
        }

        @Override
        public void onClick(View clicked) {
            EmulationActivity activity = (EmulationActivity) NativeLibrary.getEmulationContext();
            switch (mItem.getSetting()) {
                case SettingsItem.SETTING_LOAD_SUBMENU:
                    loadSubMenu(mItem.getValue());
                    break;
                case SettingsItem.SETTING_EDIT_BUTTONS:
                    activity.editControlsPlacement();
                    dismiss();
                    break;
                case SettingsItem.SETTING_SAVE_STATE:
                    NativeLibrary.SaveState(1);
                    dismiss();
                    break;
                case SettingsItem.SETTING_LOAD_STATE:
                    NativeLibrary.LoadState(1);
                    dismiss();
                    break;
                case SettingsItem.SETTING_LOAD_AMIIBO:
                    activity.loadAmiibo();
                    dismiss();
                    break;
                case SettingsItem.SETTING_REMOVE_AMIIBO:
                    activity.removeAmiibo();
                    dismiss();
                    break;
                case SettingsItem.SETTING_TOGGLE_CONTROLS:
                    activity.toggleControls();
                    dismiss();
                    break;
                case SettingsItem.SETTING_RESET_OVERLAY:
                    activity.resetOverlay();
                    dismiss();
                    break;
                case SettingsItem.SETTING_ROTATE_SCREEN:
                    activity.rotateScreen();
                    dismiss();
                    break;
                case SettingsItem.SETTING_CHEAT_CODE:
                    activity.launchCheatCode();
                    dismiss();
                    break;
                case SettingsItem.SETTING_EXIT_GAME:
                    activity.stopEmulation();
                    activity.finish();
                    break;
            }
        }
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

    public final class RadioButtonSettingViewHolder
            extends SettingViewHolder implements RadioGroup.OnCheckedChangeListener {
        SettingsItem mItem;
        private TextView mTextSettingName;
        private RadioGroup mRadioGroup;

        public RadioButtonSettingViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        protected void findViews(View root) {
            mTextSettingName = root.findViewById(R.id.text_setting_name);
            mRadioGroup = root.findViewById(R.id.radio_group);
            mRadioGroup.setOnCheckedChangeListener(this);
        }

        @Override
        public void bind(SettingsItem item) {
            int checkIds[] = {R.id.radio0, R.id.radio1, R.id.radio2, R.id.radio3};
            int index = item.getValue();
            if (index < 0 || index >= checkIds.length)
                index = 0;

            mItem = item;
            mTextSettingName.setText(item.getName());
            mRadioGroup.check(checkIds[index]);

            if (item.getSetting() == SettingsItem.SETTING_SCALE_FACTOR) {
                RadioButton radio0 = mRadioGroup.findViewById(R.id.radio0);
                radio0.setText("×1");

                RadioButton radio1 = mRadioGroup.findViewById(R.id.radio1);
                radio1.setText("×2");

                RadioButton radio2 = mRadioGroup.findViewById(R.id.radio2);
                radio2.setText("×3");

                RadioButton radio3 = mRadioGroup.findViewById(R.id.radio3);
                radio3.setVisibility(View.VISIBLE);
                radio3.setText("×4");
            } else if (item.getSetting() == SettingsItem.SETTING_SCREEN_LAYOUT) {
                RadioButton radio0 = mRadioGroup.findViewById(R.id.radio0);
                radio0.setText(R.string.default_value);

                RadioButton radio1 = mRadioGroup.findViewById(R.id.radio1);
                radio1.setText(R.string.single_screen_option);

                RadioButton radio2 = mRadioGroup.findViewById(R.id.radio2);
                radio2.setText(R.string.large_screen_option);

                RadioButton radio3 = mRadioGroup.findViewById(R.id.radio3);
                radio3.setVisibility(View.VISIBLE);
                radio3.setText(R.string.side_screen_option);
            }
        }

        @Override
        public void onClick(View clicked) {
        }

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.radio0:
                    mItem.setValue(0);
                    break;
                case R.id.radio1:
                    mItem.setValue(1);
                    break;
                case R.id.radio2:
                    mItem.setValue(2);
                    break;
                case R.id.radio3:
                    mItem.setValue(3);
                    break;
                default:
                    mItem.setValue(0);
                    break;
            }
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
        private int mHapticFeedback;
        private int mJoystickRelative;
        private int mShowOverlay;
        private int mControllerScale;
        private int mControllerAlpha;
        private int mScreenLayout;
        private ArrayList<SettingsItem> mSettings;

        public void loadMainMenu() {
            mSettings = new ArrayList<>();
            mSettings.add(new SettingsItem(SettingsItem.SETTING_LOAD_SUBMENU, R.string.preferences_settings, SettingsItem.TYPE_BUTTON, MENU_SETTINGS));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_EDIT_BUTTONS, R.string.emulation_edit_layout, SettingsItem.TYPE_BUTTON, 0));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_SAVE_STATE, R.string.emulation_save_state, SettingsItem.TYPE_BUTTON, 0));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_LOAD_STATE, R.string.emulation_load_state, SettingsItem.TYPE_BUTTON, 0));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_LOAD_AMIIBO, R.string.menu_emulation_amiibo_load, SettingsItem.TYPE_BUTTON, 0));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_REMOVE_AMIIBO, R.string.menu_emulation_amiibo_remove, SettingsItem.TYPE_BUTTON, 0));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_TOGGLE_CONTROLS, R.string.emulation_toggle_controls, SettingsItem.TYPE_BUTTON, 0));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_RESET_OVERLAY, R.string.emulation_touch_overlay_reset, SettingsItem.TYPE_BUTTON, 0));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_ROTATE_SCREEN, R.string.emulation_rotate_screen, SettingsItem.TYPE_BUTTON, 0));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_CHEAT_CODE, R.string.cheats, SettingsItem.TYPE_BUTTON, 0));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_EXIT_GAME, R.string.emulation_close_game, SettingsItem.TYPE_BUTTON, 0));
            notifyDataSetChanged();
        }

        public void loadSettingsMenu() {
            int i = 0;
            // get settings
            mRunningSettings = NativeLibrary.getRunningSettings();
            mSettings = new ArrayList<>();

            // pref settings
            mHapticFeedback = InputOverlay.sUseHapticFeedback ? 1 : 0;
            mSettings.add(new SettingsItem(SettingsItem.SETTING_HAPTIC_FEEDBACK,
                    R.string.emulation_haptic_feedback,
                    SettingsItem.TYPE_CHECKBOX, mHapticFeedback));

            mJoystickRelative = InputOverlay.sJoystickRelative ? 1 : 0;
            mSettings.add(new SettingsItem(SettingsItem.SETTING_JOYSTICK_RELATIVE,
                    R.string.emulation_control_joystick_rel_center,
                    SettingsItem.TYPE_CHECKBOX, mJoystickRelative));

            mShowOverlay = InputOverlay.sShowInputOverlay ? 1 : 0;
            mSettings.add(new SettingsItem(SettingsItem.SETTING_SHOW_OVERLAY,
                    R.string.emulation_show_overlay,
                    SettingsItem.TYPE_CHECKBOX, mShowOverlay));

            mControllerScale = InputOverlay.sControllerScale;
            mSettings.add(new SettingsItem(SettingsItem.SETTING_CONTROLLER_SCALE,
                    R.string.emulation_control_scale, SettingsItem.TYPE_SEEK_BAR,
                    mControllerScale));

            mControllerAlpha = InputOverlay.sControllerAlpha;
            mSettings.add(new SettingsItem(SettingsItem.SETTING_CONTROLLER_ALPHA,
                    R.string.emulation_control_opacity, SettingsItem.TYPE_SEEK_BAR,
                    mControllerAlpha));

            mScreenLayout = InputOverlay.sScreenLayout;
            mSettings.add(new SettingsItem(SettingsItem.SETTING_SCREEN_LAYOUT,
                    R.string.running_layout, SettingsItem.TYPE_RADIO_GROUP,
                    mScreenLayout));

            // native settings
            mSettings.add(new SettingsItem(SettingsItem.SETTING_FMV_HACK,
                    R.string.fmv_hack, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_SHOW_FPS,
                    R.string.emulation_show_fps, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_SCALE_FACTOR,
                    R.string.internal_resolution, SettingsItem.TYPE_RADIO_GROUP,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_SKIP_SLOW_DRAW,
                    R.string.setting_skip_slow_draw, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_SKIP_CPU_WRITE,
                    R.string.setting_skip_cpu_write, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_SKIP_TEXTURE_COPY,
                    R.string.setting_skip_texture_copy,
                    SettingsItem.TYPE_CHECKBOX, mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_SKIP_FORMAT_REINTERPRETATION,
                    R.string.setting_skip_format_reinterpretation, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_LINEAR_FILTER,
                    R.string.linear_filtering, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_TEXTURE_LOAD_HACK,
                    R.string.setting_texture_load_hack, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_ACCURATE_MUL,
                    R.string.shaders_accurate_mul, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_CUSTOM_LAYOUT,
                    R.string.use_custom_layout, SettingsItem.TYPE_CHECKBOX,
                    mRunningSettings[i++]));
            mSettings.add(new SettingsItem(SettingsItem.SETTING_SPEED_LIMIT,
                    R.string.frame_limit_slider, SettingsItem.TYPE_SEEK_BAR,
                    mRunningSettings[i++]));
            notifyDataSetChanged();
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
                case SettingsItem.TYPE_RADIO_GROUP:
                    itemView = inflater.inflate(R.layout.list_item_running_radio4, parent, false);
                    return new RadioButtonSettingViewHolder(itemView);
                case SettingsItem.TYPE_SEEK_BAR:
                    itemView = inflater.inflate(R.layout.list_item_running_seekbar, parent, false);
                    return new SeekBarSettingViewHolder(itemView);
                case SettingsItem.TYPE_BUTTON:
                    itemView = inflater.inflate(R.layout.list_item_running_button, parent, false);
                    return new ButtonSettingViewHolder(itemView);
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
            if (mRunningSettings == null) {
                return;
            }

            EmulationActivity activity = (EmulationActivity) NativeLibrary.getEmulationContext();
            // pref settings
            SharedPreferences.Editor editor =
                    PreferenceManager.getDefaultSharedPreferences(activity).edit();

            int feedback = mSettings.get(0).getValue();
            if (mHapticFeedback != feedback) {
                editor.putBoolean(InputOverlay.PREF_HAPTIC_FEEDBACK, feedback > 0);
                InputOverlay.sUseHapticFeedback = feedback > 0;
            }
            mSettings.remove(0);

            int relative = mSettings.get(0).getValue();
            if (mJoystickRelative != relative) {
                editor.putBoolean(InputOverlay.PREF_JOYSTICK_RELATIVE, relative > 0);
                InputOverlay.sJoystickRelative = relative > 0;
            }
            mSettings.remove(0);

            int overlay = mSettings.get(0).getValue();
            if (mShowOverlay != overlay) {
                editor.putBoolean(InputOverlay.PREF_SHOW_OVERLAY, overlay > 0);
                InputOverlay.sShowInputOverlay = overlay > 0;
            }
            mSettings.remove(0);

            int scale = mSettings.get(0).getValue();
            if (mControllerScale != scale) {
                editor.putInt(InputOverlay.PREF_CONTROLLER_SCALE, scale);
                InputOverlay.sControllerScale = scale;
            }
            mSettings.remove(0);

            int alpha = mSettings.get(0).getValue();
            if (mControllerAlpha != alpha) {
                editor.putInt(InputOverlay.PREF_CONTROLLER_ALPHA, alpha);
                InputOverlay.sControllerAlpha = alpha;
            }
            mSettings.remove(0);

            int layout = mSettings.get(0).getValue();
            if (mScreenLayout != layout) {
                editor.putInt(InputOverlay.PREF_SCREEN_LAYOUT, layout);
                InputOverlay.sScreenLayout = layout;
            }
            mSettings.remove(0);

            // apply prefs
            editor.apply();
            activity.refreshControls();

            // native settings
            boolean isChanged = false;
            int[] newSettings = new int[mRunningSettings.length];
            for (int i = 0; i < mRunningSettings.length; ++i) {
                newSettings[i] = mSettings.get(i).getValue();
                if (newSettings[i] != mRunningSettings[i]) {
                    isChanged = true;
                }
            }
            // apply settings changed
            if (isChanged) {
                NativeLibrary.setRunningSettings(newSettings);
            }
        }
    }
}