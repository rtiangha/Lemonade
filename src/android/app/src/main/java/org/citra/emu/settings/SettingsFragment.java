package org.citra.emu.settings;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.citra.emu.NativeLibrary;
import org.citra.emu.R;
import org.citra.emu.settings.model.Setting;
import org.citra.emu.settings.model.SettingSection;
import org.citra.emu.settings.model.Settings;
import org.citra.emu.settings.model.StringSetting;
import org.citra.emu.settings.view.CheckBoxSetting;
import org.citra.emu.settings.view.DateTimeSetting;
import org.citra.emu.settings.view.HeaderSetting;
import org.citra.emu.settings.view.InputBindingSetting;
import org.citra.emu.settings.view.SettingsItem;
import org.citra.emu.settings.view.SingleChoiceSetting;
import org.citra.emu.settings.view.SliderSetting;
import org.citra.emu.settings.view.StringSingleChoiceSetting;
import org.citra.emu.settings.view.SubmenuSetting;
import org.citra.emu.settings.view.ThemeSingleChoiceSetting;
import org.citra.emu.ui.DividerItemDecoration;
import org.citra.emu.utils.DirectoryInitialization;
import org.citra.emu.utils.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class SettingsFragment extends Fragment {
    private static final String ARGUMENT_MENU_TAG = "menu_tag";
    private static final String ARGUMENT_GAME_ID = "game_id";

    private SettingsActivity mActivity;

    private String mMenuTag;
    private String mGameID;

    private Settings mSettings;
    private ArrayList<SettingsItem> mSettingsList;

    private SettingsAdapter mAdapter;

    public static Fragment newInstance(String menuTag, String gameId) {
        SettingsFragment fragment = new SettingsFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_MENU_TAG, menuTag);
        arguments.putString(ARGUMENT_GAME_ID, gameId);

        fragment.setArguments(arguments);
        return fragment;
    }

    public void onCreate(String menuTag, String gameId) {
        mGameID = gameId;
        mMenuTag = menuTag;
    }

    public void onViewCreated(Settings settings) {
        setSettings(settings);
    }

    /**
     * If the screen is rotated, the Activity will forget the settings map. This fragment
     * won't, though; so rather than have the Activity reload from disk, have the fragment pass
     * the settings map back to the Activity.
     */
    public void onAttach() {
        if (mSettings != null) {
            passSettingsToActivity(mSettings);
        }
    }

    public void putSetting(Setting setting) {
        mSettings.getSection(setting.getSection()).putSetting(setting);
    }

    private StringSetting asStringSetting(Setting setting) {
        if (setting == null) {
            return null;
        }

        StringSetting stringSetting = new StringSetting(setting.getKey(), setting.getSection(), setting.getValueAsString());
        putSetting(stringSetting);
        return stringSetting;
    }

    public void loadDefaultSettings() {
        loadSettingsList();
    }

    public void setSettings(Settings settings) {
        if (mSettingsList == null && settings != null) {
            mSettings = settings;

            loadSettingsList();
        } else {
            getActivity().setTitle(R.string.preferences_settings);
            showSettingsList(mSettingsList);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mActivity = (SettingsActivity) context;
        onAttach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        String menuTag = getArguments().getString(ARGUMENT_MENU_TAG);
        String gameId = getArguments().getString(ARGUMENT_GAME_ID);

        mAdapter = new SettingsAdapter(this, getActivity());

        onCreate(menuTag, gameId);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());

        RecyclerView recyclerView = view.findViewById(R.id.list_settings);

        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(manager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), null));

        SettingsActivity activity = (SettingsActivity) getActivity();

        onViewCreated(activity.getSettings());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;

        if (mAdapter != null) {
            mAdapter.closeDialog();
        }
    }

    public void onSettingsFileLoaded(Settings settings) {
        setSettings(settings);
    }

    public void passSettingsToActivity(Settings settings) {
        if (mActivity != null) {
            mActivity.setSettings(settings);
        }
    }

    public void showSettingsList(ArrayList<SettingsItem> settingsList) {
        mAdapter.setSettings(settingsList);
    }

    public void loadSubMenu(String menuKey) {
        mActivity.showSettingsFragment(menuKey, true, getArguments().getString(ARGUMENT_GAME_ID));
    }

    public void showToastMessage(String message, boolean is_long) {
        mActivity.showToastMessage(message, is_long);
    }

    public void onSettingChanged() {
        mActivity.onSettingChanged();
    }

    private void loadSettingsList() {
        if (!TextUtils.isEmpty(mGameID)) {
            getActivity().setTitle("Game Settings: " + mGameID);
        }
        ArrayList<SettingsItem> sl = new ArrayList<>();

        if (mMenuTag == null) {
            return;
        }

        switch (mMenuTag) {
            case SettingsFile.FILE_NAME_CONFIG:
                addConfigSettings(sl);
                break;
            case Settings.SECTION_INTERFACE:
                addInterfaceSettings(sl);
                break;
            case Settings.SECTION_CORE:
                addGeneralSettings(sl);
                break;
            case Settings.SECTION_CUSTOM_TEXTURES:
                addCustomTexturesSettings(sl);
                break;
            case Settings.SECTION_SYSTEM:
                addSystemSettings(sl);
                break;
            case Settings.SECTION_CAMERA:
                addCameraSettings(sl);
                break;
            case Settings.SECTION_CONTROLS:
                addInputSettings(sl);
                break;
            case Settings.SECTION_RENDERER:
                addGraphicsSettings(sl);
                break;
            case Settings.SECTION_AUDIO:
                addAudioSettings(sl);
                break;
            case Settings.SECTION_DEBUG:
                addDebugSettings(sl);
                break;
            default:
                showToastMessage("Unimplemented menu", false);
                return;
        }

        mSettingsList = sl;
        showSettingsList(mSettingsList);
    }

    private void addConfigSettings(ArrayList<SettingsItem> sl) {
        getActivity().setTitle(R.string.preferences_settings);

        sl.add(new SubmenuSetting(null, null, R.string.preferences_general, 0, Settings.SECTION_CORE));
        sl.add(new SubmenuSetting(null, null, R.string.preferences_system, 0, Settings.SECTION_SYSTEM));
        sl.add(new SubmenuSetting(null, null, R.string.preferences_camera, 0, Settings.SECTION_CAMERA));
        sl.add(new SubmenuSetting(null, null, R.string.preferences_interface, 0, Settings.SECTION_INTERFACE));
        sl.add(new SubmenuSetting(null, null, R.string.preferences_controls, 0, Settings.SECTION_CONTROLS));
        sl.add(new SubmenuSetting(null, null, R.string.preferences_graphics, 0, Settings.SECTION_RENDERER));
        sl.add(new SubmenuSetting(null, null, R.string.preferences_audio, 0, Settings.SECTION_AUDIO));
        sl.add(new SubmenuSetting(null, null, R.string.preferences_debug, 0, Settings.SECTION_DEBUG));
    }

    private void addInterfaceSettings(ArrayList<SettingsItem> sl) {
        getActivity().setTitle(R.string.preferences_interface);

        SettingSection interfaceSection = mSettings.getSection(Settings.SECTION_INTERFACE);
        Setting checkUpdates = interfaceSection.getSetting(SettingsFile.KEY_UPDATER_CHECK_AT_STARTUP);
        Setting design = interfaceSection.getSetting(SettingsFile.KEY_DESIGN);

        sl.add(new CheckBoxSetting(SettingsFile.KEY_UPDATER_CHECK_AT_STARTUP, Settings.SECTION_INTERFACE, R.string.updater_check_startup, R.string.updater_check_startup_description, true, checkUpdates));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            sl.add(new ThemeSingleChoiceSetting(SettingsFile.KEY_DESIGN, Settings.SECTION_INTERFACE, R.string.design, 0, R.array.designNames, R.array.designValues, 0, design, this));
        } else {
            // Pre-Android 10 does not support System Default
            sl.add(new ThemeSingleChoiceSetting(SettingsFile.KEY_DESIGN, Settings.SECTION_INTERFACE, R.string.design, 0, R.array.designNamesOld, R.array.designValuesOld, 0, design, this));
        }

        String[] textureFilterNames = NativeLibrary.GetTextureFilterNames();
        Setting textureFilterName = interfaceSection.getSetting(SettingsFile.KEY_TEXTURE_FILTER_NAME);
        sl.add(new StringSingleChoiceSetting(SettingsFile.KEY_TEXTURE_FILTER_NAME, Settings.SECTION_INTERFACE, R.string.texture_filter_name, R.string.texture_filter_description, textureFilterNames, textureFilterNames, "none", textureFilterName));
    }

    private void addGeneralSettings(ArrayList<SettingsItem> sl) {
        getActivity().setTitle(R.string.preferences_general);

        SettingSection rendererSection = mSettings.getSection(Settings.SECTION_RENDERER);
        SettingSection coreSection = mSettings.getSection(Settings.SECTION_CORE);
        Setting frameLimitEnable = rendererSection.getSetting(SettingsFile.KEY_FRAME_LIMIT_ENABLED);
        Setting cpuClockSpeed = coreSection.getSetting(SettingsFile.KEY_CPU_CLOCK_SPEED);
        Setting frameLimitValue = rendererSection.getSetting(SettingsFile.KEY_FRAME_LIMIT);

        sl.add(new CheckBoxSetting(SettingsFile.KEY_FRAME_LIMIT_ENABLED, Settings.SECTION_RENDERER, R.string.frame_limit_enable, R.string.frame_limit_enable_description, true, frameLimitEnable));
        sl.add(new SliderSetting(SettingsFile.KEY_CPU_CLOCK_SPEED, Settings.SECTION_CORE, R.string.cpu_clock_speed, 0, 0, 400, "%", 100, cpuClockSpeed));
        sl.add(new SliderSetting(SettingsFile.KEY_FRAME_LIMIT, Settings.SECTION_RENDERER, R.string.frame_limit_slider, R.string.frame_limit_slider_description, 1, 200, "%", 100, frameLimitValue));
    }

    private void addSystemSettings(ArrayList<SettingsItem> sl) {
        getActivity().setTitle(R.string.preferences_system);

        SettingSection systemSection = mSettings.getSection(Settings.SECTION_SYSTEM);
        Setting region = systemSection.getSetting(SettingsFile.KEY_REGION_VALUE);
        Setting language = systemSection.getSetting(SettingsFile.KEY_LANGUAGE);
        Setting systemClock = systemSection.getSetting(SettingsFile.KEY_INIT_CLOCK);
        Setting dateTime = systemSection.getSetting(SettingsFile.KEY_INIT_TIME);

        sl.add(new SingleChoiceSetting(SettingsFile.KEY_REGION_VALUE, Settings.SECTION_SYSTEM, R.string.emulated_region, 0, R.array.regionNames, R.array.regionValues, -1, region));
        sl.add(new SingleChoiceSetting(SettingsFile.KEY_LANGUAGE, Settings.SECTION_SYSTEM, R.string.emulated_language, 0, R.array.languageNames, R.array.languageValues, 1, language));
        sl.add(new SingleChoiceSetting(SettingsFile.KEY_INIT_CLOCK, Settings.SECTION_SYSTEM, R.string.init_clock, R.string.init_clock_description, R.array.systemClockNames, R.array.systemClockValues, 0, systemClock));
        sl.add(new DateTimeSetting(SettingsFile.KEY_INIT_TIME, Settings.SECTION_SYSTEM, R.string.init_time, R.string.init_time_description, "2000-01-01 00:00:01", dateTime));
    }

    private void addCameraSettings(ArrayList<SettingsItem> sl) {
        getActivity().setTitle(R.string.preferences_camera);

        // Get the camera IDs
        CameraManager cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        ArrayList<String> supportedCameraNameList = new ArrayList<>();
        ArrayList<String> supportedCameraIdList = new ArrayList<>();
        if (cameraManager != null) {
            try {
                for (String id : cameraManager.getCameraIdList()) {
                    final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    if (Objects.requireNonNull(characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        continue; // Legacy cameras cannot be used with the NDK
                    }

                    supportedCameraIdList.add(id);

                    final int facing = Objects.requireNonNull(characteristics.get(CameraCharacteristics.LENS_FACING));
                    int stringId = R.string.camera_facing_external;
                    switch (facing) {
                        case CameraCharacteristics.LENS_FACING_FRONT:
                            stringId = R.string.camera_facing_front;
                            break;
                        case CameraCharacteristics.LENS_FACING_BACK:
                            stringId = R.string.camera_facing_back;
                            break;
                        case CameraCharacteristics.LENS_FACING_EXTERNAL:
                            stringId = R.string.camera_facing_external;
                            break;
                    }
                    supportedCameraNameList.add(String.format("%1$s (%2$s)", id, getString(stringId)));
                }
            } catch (CameraAccessException e) {
                Log.error("Couldn't retrieve camera list");
                e.printStackTrace();
            }
        }

        // Create the names and values for display
        ArrayList<String> cameraDeviceNameList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.cameraDeviceNames)));
        cameraDeviceNameList.addAll(supportedCameraNameList);
        ArrayList<String> cameraDeviceValueList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.cameraDeviceValues)));
        cameraDeviceValueList.addAll(supportedCameraIdList);

        final String[] cameraDeviceNames = cameraDeviceNameList.toArray(new String[]{});
        final String[] cameraDeviceValues = cameraDeviceValueList.toArray(new String[]{});

        final boolean haveCameraDevices = !supportedCameraIdList.isEmpty();

        String[] imageSourceNames = getResources().getStringArray(R.array.cameraImageSourceNames);
        String[] imageSourceValues = getResources().getStringArray(R.array.cameraImageSourceValues);
        if (!haveCameraDevices) {
            // Remove the last entry (ndk / Device Camera)
            imageSourceNames = Arrays.copyOfRange(imageSourceNames, 0, imageSourceNames.length - 1);
            imageSourceValues = Arrays.copyOfRange(imageSourceValues, 0, imageSourceValues.length - 1);
        }

        final String defaultImageSource = haveCameraDevices ? "ndk" : "image";

        SettingSection cameraSection = mSettings.getSection(Settings.SECTION_CAMERA);

        Setting innerCameraImageSource = cameraSection.getSetting(SettingsFile.KEY_CAMERA_INNER_NAME);
        Setting innerCameraConfig = asStringSetting(cameraSection.getSetting(SettingsFile.KEY_CAMERA_INNER_CONFIG));
        Setting innerCameraFlip = cameraSection.getSetting(SettingsFile.KEY_CAMERA_INNER_FLIP);
        sl.add(new HeaderSetting(null, null, R.string.inner_camera, 0));
        sl.add(new StringSingleChoiceSetting(SettingsFile.KEY_CAMERA_INNER_NAME, Settings.SECTION_CAMERA, R.string.image_source, R.string.image_source_description, imageSourceNames, imageSourceValues, defaultImageSource, innerCameraImageSource));
        if (haveCameraDevices)
            sl.add(new StringSingleChoiceSetting(SettingsFile.KEY_CAMERA_INNER_CONFIG, Settings.SECTION_CAMERA, R.string.camera_device, R.string.camera_device_description, cameraDeviceNames, cameraDeviceValues, "_front", innerCameraConfig));
        sl.add(new SingleChoiceSetting(SettingsFile.KEY_CAMERA_INNER_FLIP, Settings.SECTION_CAMERA, R.string.image_flip, 0, R.array.cameraFlipNames, R.array.cameraFlipValues, 0, innerCameraFlip));

        Setting outerLeftCameraImageSource = cameraSection.getSetting(SettingsFile.KEY_CAMERA_OUTER_LEFT_NAME);
        Setting outerLeftCameraConfig = asStringSetting(cameraSection.getSetting(SettingsFile.KEY_CAMERA_OUTER_LEFT_CONFIG));
        Setting outerLeftCameraFlip = cameraSection.getSetting(SettingsFile.KEY_CAMERA_OUTER_LEFT_FLIP);
        sl.add(new HeaderSetting(null, null, R.string.outer_left_camera, 0));
        sl.add(new StringSingleChoiceSetting(SettingsFile.KEY_CAMERA_OUTER_LEFT_NAME, Settings.SECTION_CAMERA, R.string.image_source, R.string.image_source_description, imageSourceNames, imageSourceValues, defaultImageSource, outerLeftCameraImageSource));
        if (haveCameraDevices)
            sl.add(new StringSingleChoiceSetting(SettingsFile.KEY_CAMERA_OUTER_LEFT_CONFIG, Settings.SECTION_CAMERA, R.string.camera_device, R.string.camera_device_description, cameraDeviceNames, cameraDeviceValues, "_back", outerLeftCameraConfig));
        sl.add(new SingleChoiceSetting(SettingsFile.KEY_CAMERA_OUTER_LEFT_FLIP, Settings.SECTION_CAMERA, R.string.image_flip, 0, R.array.cameraFlipNames, R.array.cameraFlipValues, 0, outerLeftCameraFlip));

        Setting outerRightCameraImageSource = cameraSection.getSetting(SettingsFile.KEY_CAMERA_OUTER_RIGHT_NAME);
        Setting outerRightCameraConfig = asStringSetting(cameraSection.getSetting(SettingsFile.KEY_CAMERA_OUTER_RIGHT_CONFIG));
        Setting outerRightCameraFlip = cameraSection.getSetting(SettingsFile.KEY_CAMERA_OUTER_RIGHT_FLIP);
        sl.add(new HeaderSetting(null, null, R.string.outer_right_camera, 0));
        sl.add(new StringSingleChoiceSetting(SettingsFile.KEY_CAMERA_OUTER_RIGHT_NAME, Settings.SECTION_CAMERA, R.string.image_source, R.string.image_source_description, imageSourceNames, imageSourceValues, defaultImageSource, outerRightCameraImageSource));
        if (haveCameraDevices)
            sl.add(new StringSingleChoiceSetting(SettingsFile.KEY_CAMERA_OUTER_RIGHT_CONFIG, Settings.SECTION_CAMERA, R.string.camera_device, R.string.camera_device_description, cameraDeviceNames, cameraDeviceValues, "_back", outerRightCameraConfig));
        sl.add(new SingleChoiceSetting(SettingsFile.KEY_CAMERA_OUTER_RIGHT_FLIP, Settings.SECTION_CAMERA, R.string.image_flip, 0, R.array.cameraFlipNames, R.array.cameraFlipValues, 0, outerRightCameraFlip));
    }

    private void addInputSettings(ArrayList<SettingsItem> sl) {
        getActivity().setTitle(R.string.preferences_controls);

        SettingSection controlsSection = mSettings.getSection(Settings.SECTION_CONTROLS);
        Setting buttonA = controlsSection.getSetting(SettingsFile.KEY_BUTTON_A);
        Setting buttonB = controlsSection.getSetting(SettingsFile.KEY_BUTTON_B);
        Setting buttonX = controlsSection.getSetting(SettingsFile.KEY_BUTTON_X);
        Setting buttonY = controlsSection.getSetting(SettingsFile.KEY_BUTTON_Y);
        Setting buttonSelect = controlsSection.getSetting(SettingsFile.KEY_BUTTON_SELECT);
        Setting buttonStart = controlsSection.getSetting(SettingsFile.KEY_BUTTON_START);
        Setting circlepadAxisVert = controlsSection.getSetting(SettingsFile.KEY_CIRCLEPAD_AXIS_VERTICAL);
        Setting circlepadAxisHoriz = controlsSection.getSetting(SettingsFile.KEY_CIRCLEPAD_AXIS_HORIZONTAL);
        Setting cstickAxisVert = controlsSection.getSetting(SettingsFile.KEY_CSTICK_AXIS_VERTICAL);
        Setting cstickAxisHoriz = controlsSection.getSetting(SettingsFile.KEY_CSTICK_AXIS_HORIZONTAL);
        Setting dpadAxisVert = controlsSection.getSetting(SettingsFile.KEY_DPAD_AXIS_VERTICAL);
        Setting dpadAxisHoriz = controlsSection.getSetting(SettingsFile.KEY_DPAD_AXIS_HORIZONTAL);
        // Setting buttonUp = controlsSection.getSetting(SettingsFile.KEY_BUTTON_UP);
        // Setting buttonDown = controlsSection.getSetting(SettingsFile.KEY_BUTTON_DOWN);
        // Setting buttonLeft = controlsSection.getSetting(SettingsFile.KEY_BUTTON_LEFT);
        // Setting buttonRight = controlsSection.getSetting(SettingsFile.KEY_BUTTON_RIGHT);
        Setting buttonL = controlsSection.getSetting(SettingsFile.KEY_BUTTON_L);
        Setting buttonR = controlsSection.getSetting(SettingsFile.KEY_BUTTON_R);
        Setting buttonZL = controlsSection.getSetting(SettingsFile.KEY_BUTTON_ZL);
        Setting buttonZR = controlsSection.getSetting(SettingsFile.KEY_BUTTON_ZR);

        sl.add(new HeaderSetting(null, null, R.string.generic_buttons, 0));
        sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_A, Settings.SECTION_CONTROLS, R.string.button_a, buttonA));
        sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_B, Settings.SECTION_CONTROLS, R.string.button_b, buttonB));
        sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_X, Settings.SECTION_CONTROLS, R.string.button_x, buttonX));
        sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_Y, Settings.SECTION_CONTROLS, R.string.button_y, buttonY));
        sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_SELECT, Settings.SECTION_CONTROLS, R.string.button_select, buttonSelect));
        sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_START, Settings.SECTION_CONTROLS, R.string.button_start, buttonStart));

        sl.add(new HeaderSetting(null, null, R.string.controller_circlepad, 0));
        sl.add(new InputBindingSetting(SettingsFile.KEY_CIRCLEPAD_AXIS_VERTICAL, Settings.SECTION_CONTROLS, R.string.controller_axis_vertical, circlepadAxisVert));
        sl.add(new InputBindingSetting(SettingsFile.KEY_CIRCLEPAD_AXIS_HORIZONTAL, Settings.SECTION_CONTROLS, R.string.controller_axis_horizontal, circlepadAxisHoriz));

        sl.add(new HeaderSetting(null, null, R.string.controller_c, 0));
        sl.add(new InputBindingSetting(SettingsFile.KEY_CSTICK_AXIS_VERTICAL, Settings.SECTION_CONTROLS, R.string.controller_axis_vertical, cstickAxisVert));
        sl.add(new InputBindingSetting(SettingsFile.KEY_CSTICK_AXIS_HORIZONTAL, Settings.SECTION_CONTROLS, R.string.controller_axis_horizontal, cstickAxisHoriz));

        sl.add(new HeaderSetting(null, null, R.string.controller_dpad, 0));
        sl.add(new InputBindingSetting(SettingsFile.KEY_DPAD_AXIS_VERTICAL, Settings.SECTION_CONTROLS, R.string.controller_axis_vertical, dpadAxisVert));
        sl.add(new InputBindingSetting(SettingsFile.KEY_DPAD_AXIS_HORIZONTAL, Settings.SECTION_CONTROLS, R.string.controller_axis_horizontal, dpadAxisHoriz));

        // TODO(bunnei): Figure out what to do with these. Configuring is functional, but removing for MVP because they are confusing.
        // sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_UP, Settings.SECTION_CONTROLS, R.string.generic_up, buttonUp));
        // sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_DOWN, Settings.SECTION_CONTROLS, R.string.generic_down, buttonDown));
        // sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_LEFT, Settings.SECTION_CONTROLS, R.string.generic_left, buttonLeft));
        // sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_RIGHT, Settings.SECTION_CONTROLS, R.string.generic_right, buttonRight));

        sl.add(new HeaderSetting(null, null, R.string.controller_triggers, 0));
        sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_L, Settings.SECTION_CONTROLS, R.string.button_l, buttonL));
        sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_R, Settings.SECTION_CONTROLS, R.string.button_r, buttonR));
        sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_ZL, Settings.SECTION_CONTROLS, R.string.button_zl, buttonZL));
        sl.add(new InputBindingSetting(SettingsFile.KEY_BUTTON_ZR, Settings.SECTION_CONTROLS, R.string.button_zr, buttonZR));
    }

    private void addGraphicsSettings(ArrayList<SettingsItem> sl) {
        getActivity().setTitle(R.string.preferences_graphics);

        SettingSection rendererSection = mSettings.getSection(Settings.SECTION_RENDERER);
        SettingSection systemSection = mSettings.getSection(Settings.SECTION_SYSTEM);

        Setting presentThread = rendererSection.getSetting(SettingsFile.KEY_USE_PRESENT_THREAD);
        Setting new3ds = systemSection.getSetting(SettingsFile.KEY_IS_NEW_3DS);
        Setting resolutionFactor = rendererSection.getSetting(SettingsFile.KEY_RESOLUTION_FACTOR);
        Setting showFps = rendererSection.getSetting(SettingsFile.KEY_SHOW_FPS);
        Setting cpuUsageLimit = rendererSection.getSetting(SettingsFile.KEY_CPU_USAGE_LIMIT);
        Setting textureLoadHack = rendererSection.getSetting(SettingsFile.KEY_TEXTURE_LOAD_HACK);
        // Setting filterMode = rendererSection.getSetting(SettingsFile.KEY_FILTER_MODE);
        Setting useAsynchronousGpuEmulation = rendererSection.getSetting(SettingsFile.KEY_USE_ASYNCHRONOUS_GPU_EMULATION);
        Setting shadersAccurateMul = rendererSection.getSetting(SettingsFile.KEY_SHADERS_ACCURATE_MUL);
        Setting shader = rendererSection.getSetting(SettingsFile.KEY_PP_SHADER_NAME);
        Setting render3dMode = rendererSection.getSetting(SettingsFile.KEY_RENDER_3D);
        Setting factor3d = rendererSection.getSetting(SettingsFile.KEY_FACTOR_3D);
        Setting useDiskShaderCache = rendererSection.getSetting(SettingsFile.KEY_USE_DISK_SHADER_CACHE);

        SettingSection layoutSection = mSettings.getSection(Settings.SECTION_LAYOUT);
        Setting cardboardScreenSize = layoutSection.getSetting(SettingsFile.KEY_CARDBOARD_SCREEN_SIZE);
        Setting cardboardXShift = layoutSection.getSetting(SettingsFile.KEY_CARDBOARD_X_SHIFT);
        Setting cardboardYShift = layoutSection.getSetting(SettingsFile.KEY_CARDBOARD_Y_SHIFT);

        sl.add(new HeaderSetting(null, null, R.string.renderer, 0));
        sl.add(new CheckBoxSetting(SettingsFile.KEY_USE_PRESENT_THREAD, Settings.SECTION_RENDERER, R.string.setting_use_present_thread, R.string.setting_use_present_thread_description, true, presentThread));
        sl.add(new CheckBoxSetting(SettingsFile.KEY_IS_NEW_3DS, Settings.SECTION_SYSTEM, R.string.setting_is_new_3ds, R.string.setting_is_new_3ds_description, false, new3ds));
        sl.add(new SliderSetting(SettingsFile.KEY_RESOLUTION_FACTOR, Settings.SECTION_RENDERER, R.string.internal_resolution, R.string.internal_resolution_description, 1, 4, "x", 1, resolutionFactor));
        sl.add(new CheckBoxSetting(SettingsFile.KEY_SHOW_FPS, Settings.SECTION_RENDERER, R.string.emulation_show_fps, 0, false, showFps));
        sl.add(new CheckBoxSetting(SettingsFile.KEY_CPU_USAGE_LIMIT, Settings.SECTION_RENDERER, R.string.cpu_usage_limit, R.string.cpu_usage_limit_description, false, cpuUsageLimit));
        sl.add(new CheckBoxSetting(SettingsFile.KEY_TEXTURE_LOAD_HACK, Settings.SECTION_RENDERER, R.string.setting_texture_load_hack, R.string.setting_texture_load_hack_description, false, textureLoadHack));
        // TODO(Gamer64): hide custom textures for now, crashes like official.
        // sl.add(new SubmenuSetting(null, null, R.string.setting_custom_textures_title, 0, Settings.SECTION_CUSTOM_TEXTURES));
        sl.add(new CheckBoxSetting(SettingsFile.KEY_USE_ASYNCHRONOUS_GPU_EMULATION, Settings.SECTION_RENDERER, R.string.asynchronous_gpu, R.string.asynchronous_gpu_description, true, useAsynchronousGpuEmulation));
        sl.add(new CheckBoxSetting(SettingsFile.KEY_SHADERS_ACCURATE_MUL, Settings.SECTION_RENDERER, R.string.shaders_accurate_mul, R.string.shaders_accurate_mul_description, false, shadersAccurateMul));

        // post process shaders
        String[] shaderListEntries = getShaderList();
        String[] shaderListValues = new String[shaderListEntries.length];
        System.arraycopy(shaderListEntries, 0, shaderListValues, 0, shaderListEntries.length);
        sl.add(new StringSingleChoiceSetting(SettingsFile.KEY_PP_SHADER_NAME,
                Settings.SECTION_RENDERER, R.string.post_processing_shader,
                0, shaderListEntries, shaderListValues, "",
                shader));

        sl.add(new CheckBoxSetting(SettingsFile.KEY_USE_DISK_SHADER_CACHE, Settings.SECTION_RENDERER, R.string.use_disk_shader_cache, R.string.use_disk_shader_cache_description, true, useDiskShaderCache));

        sl.add(new HeaderSetting(null, null, R.string.stereoscopy, 0));
        sl.add(new SingleChoiceSetting(SettingsFile.KEY_RENDER_3D, Settings.SECTION_RENDERER, R.string.render3d, 0, R.array.render3dModes, R.array.render3dValues, 0, render3dMode));
        sl.add(new SliderSetting(SettingsFile.KEY_FACTOR_3D, Settings.SECTION_RENDERER, R.string.factor3d, R.string.factor3d_description, 0, 100, "%", 0, factor3d));

        sl.add(new HeaderSetting(null, null, R.string.cardboard_vr, 0));
        sl.add(new SliderSetting(SettingsFile.KEY_CARDBOARD_SCREEN_SIZE, Settings.SECTION_LAYOUT, R.string.cardboard_screen_size, R.string.cardboard_screen_size_description, 30, 100, "%", 85, cardboardScreenSize));
        sl.add(new SliderSetting(SettingsFile.KEY_CARDBOARD_X_SHIFT, Settings.SECTION_LAYOUT, R.string.cardboard_x_shift, R.string.cardboard_x_shift_description, -100, 100, "%", 0, cardboardXShift));
        sl.add(new SliderSetting(SettingsFile.KEY_CARDBOARD_Y_SHIFT, Settings.SECTION_LAYOUT, R.string.cardboard_y_shift, R.string.cardboard_y_shift_description, -100, 100, "%", 0, cardboardYShift));
    }

    private void addCustomTexturesSettings(ArrayList<SettingsItem> sl) {
        getActivity().setTitle(R.string.setting_custom_textures_title);

        SettingSection rendererSection = mSettings.getSection(Settings.SECTION_RENDERER);
        Setting customTextures = rendererSection.getSetting(SettingsFile.KEY_CUSTOM_TEXTURES);
        Setting preloadTextures = rendererSection.getSetting(SettingsFile.KEY_PRELOAD_TEXTURES);
        // Setting dumpTextures = rendererSection.getSetting(SettingsFile.KEY_DUMP_TEXTURES);

        sl.add(new CheckBoxSetting(SettingsFile.KEY_CUSTOM_TEXTURES, Settings.SECTION_RENDERER, R.string.setting_custom_textures, 0, false, customTextures));
        sl.add(new CheckBoxSetting(SettingsFile.KEY_PRELOAD_TEXTURES, Settings.SECTION_RENDERER, R.string.setting_preload_textures, R.string.setting_preload_textures_description, false, preloadTextures));
        // TODO(Gamer64): dump textures don't works for now
        // sl.add(new CheckBoxSetting(SettingsFile.KEY_DUMP_TEXTURES, Settings.SECTION_RENDERER, R.string.setting_dump_textures, 0, false, dumpTextures));
    }

    private void addAudioSettings(ArrayList<SettingsItem> sl) {
        getActivity().setTitle(R.string.preferences_audio);

        SettingSection audioSection = mSettings.getSection(Settings.SECTION_AUDIO);
        Setting audioStretch = audioSection.getSetting(SettingsFile.KEY_ENABLE_AUDIO_STRETCHING);
        Setting micInputType = audioSection.getSetting(SettingsFile.KEY_MIC_INPUT_TYPE);

        sl.add(new CheckBoxSetting(SettingsFile.KEY_ENABLE_AUDIO_STRETCHING, Settings.SECTION_AUDIO, R.string.audio_stretch, R.string.audio_stretch_description, true, audioStretch));
        sl.add(new SingleChoiceSetting(SettingsFile.KEY_MIC_INPUT_TYPE, Settings.SECTION_AUDIO, R.string.audio_input_type, 0, R.array.audioInputTypeNames, R.array.audioInputTypeValues, 1, micInputType));
    }

    private void addDebugSettings(ArrayList<SettingsItem> sl) {
        getActivity().setTitle(R.string.preferences_debug);

        SettingSection coreSection = mSettings.getSection(Settings.SECTION_CORE);
        SettingSection rendererSection = mSettings.getSection(Settings.SECTION_RENDERER);
        Setting useCpuJit = coreSection.getSetting(SettingsFile.KEY_CPU_JIT);
        Setting hardwareRenderer = rendererSection.getSetting(SettingsFile.KEY_HW_RENDERER);
        Setting hardwareShader = rendererSection.getSetting(SettingsFile.KEY_HW_SHADER);
        Setting vsyncEnable = rendererSection.getSetting(SettingsFile.KEY_USE_VSYNC);

        sl.add(new HeaderSetting(null, null, R.string.debug_warning, 0));
        sl.add(new CheckBoxSetting(SettingsFile.KEY_CPU_JIT, Settings.SECTION_CORE, R.string.cpu_jit, R.string.cpu_jit_description, true, useCpuJit, true, this));
        sl.add(new CheckBoxSetting(SettingsFile.KEY_HW_RENDERER, Settings.SECTION_RENDERER, R.string.hw_renderer, R.string.hw_renderer_description, true, hardwareRenderer, true, this));
        sl.add(new CheckBoxSetting(SettingsFile.KEY_HW_SHADER, Settings.SECTION_RENDERER, R.string.hw_shaders, R.string.hw_shaders_description, true, hardwareShader, true, this));
        sl.add(new CheckBoxSetting(SettingsFile.KEY_USE_VSYNC, Settings.SECTION_RENDERER, R.string.vsync, R.string.vsync_description, true, vsyncEnable));
    }

    private String[] getShaderList() {
        try {
            String shadersPath =
                    DirectoryInitialization.getUserDirectory() + "/shaders";

            File file = new File(shadersPath);
            File[] shaderFiles = file.listFiles();
            if (shaderFiles != null) {
                String[] result = new String[shaderFiles.length + 1];
                result[0] = getActivity().getString(R.string.off);
                for (int i = 0; i < shaderFiles.length; i++) {
                    String name = shaderFiles[i].getName();
                    int extensionIndex = name.indexOf(".glsl");
                    if (extensionIndex > 0) {
                        name = name.substring(0, extensionIndex);
                    }
                    result[i + 1] = name;
                }

                return result;
            }
        } catch (Exception ex) {
            Log.debug("[Settings] Unable to find shader files");
            // return empty list
        }

        return new String[]{};
    }
}
