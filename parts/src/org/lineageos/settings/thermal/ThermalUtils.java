/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.thermal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserHandle;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public final class ThermalUtils {

    private static final String THERMAL_CONTROL = "thermal_control";
    private static final String KEY_APP_STATE_PREFIX = "thermal_app_state_";

    public static final int STATE_DEFAULT = 0;
    public static final int STATE_BENCHMARK = 1;
    public static final int STATE_BROWSER = 2;
    public static final int STATE_CAMERA = 3;
    public static final int STATE_DIALER = 4;
    public static final int STATE_GAMING = 5;
    public static final int STATE_STREAMING = 6;
    public static final int STATE_HEAVY_GAMING = 7;
    public static final int STATE_EXTREME_GAMING = 8;
    public static final int STATE_NAVIGATION = 9;
    public static final int STATE_VIDEO_CALL = 10;
    public static final int STATE_VIDEO_PLAYBACK = 11;
    public static final int STATE_4K_RECORDING = 12;
    public static final int STATE_ARVR = 13;
    public static final int STATE_COOL = 14;

    public static final int PROFILE_COUNT = 15;

    private static final int[] PROFILE_NAME_RES = {
            R.string.thermal_default,
            R.string.thermal_benchmark,
            R.string.thermal_browser,
            R.string.thermal_camera,
            R.string.thermal_dialer,
            R.string.thermal_gaming,
            R.string.thermal_streaming,
            R.string.thermal_heavy_gaming,
            R.string.thermal_extreme_gaming,
            R.string.thermal_navigation,
            R.string.thermal_video_call,
            R.string.thermal_video_playback,
            R.string.thermal_4k_recording,
            R.string.thermal_arvr,
            R.string.thermal_cool
    };

    private static final int[] PROFILE_DESC_RES = {
            R.string.thermal_default_desc,
            R.string.thermal_benchmark_desc,
            R.string.thermal_browser_desc,
            R.string.thermal_camera_desc,
            R.string.thermal_dialer_desc,
            R.string.thermal_gaming_desc,
            R.string.thermal_streaming_desc,
            R.string.thermal_heavy_gaming_desc,
            R.string.thermal_extreme_gaming_desc,
            R.string.thermal_navigation_desc,
            R.string.thermal_video_call_desc,
            R.string.thermal_video_playback_desc,
            R.string.thermal_4k_recording_desc,
            R.string.thermal_arvr_desc,
            R.string.thermal_cool_desc
    };

    public static int getProfileNameRes(int state) {
        return PROFILE_NAME_RES[state >= 0 && state < PROFILE_COUNT ? state : STATE_DEFAULT];
    }

    public static int getProfileDescRes(int state) {
        return PROFILE_DESC_RES[state >= 0 && state < PROFILE_COUNT ? state : STATE_DEFAULT];
    }

    public static boolean isProfileAvailable(int state) {
        return ThermalMapResolver.getInstance().isProfileAvailable(state);
    }

    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";

    private final SharedPreferences mSharedPrefs;
    private final ThermalMapResolver mMapResolver;

    protected ThermalUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mMapResolver = ThermalMapResolver.getInstance();
    }

    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, ThermalService.class),
                UserHandle.CURRENT);
    }

    protected void writePackage(String packageName, int mode) {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        if (mode <= STATE_DEFAULT || mode >= PROFILE_COUNT) {
            editor.remove(KEY_APP_STATE_PREFIX + packageName);
        } else {
            editor.putInt(KEY_APP_STATE_PREFIX + packageName, mode);
        }
        // Remove any legacy entry for this package so it cannot override the new setting
        String legacy = mSharedPrefs.getString(THERMAL_CONTROL, null);
        if (legacy != null && legacy.contains(packageName + ",")) {
            editor.putString(THERMAL_CONTROL, legacy.replace(packageName + ",", ""));
        }
        editor.apply();
    }

    protected int getStateForPackage(String packageName) {
        if (packageName == null) {
            return STATE_DEFAULT;
        }
        String key = KEY_APP_STATE_PREFIX + packageName;
        if (mSharedPrefs.contains(key)) {
            int state = mSharedPrefs.getInt(key, STATE_DEFAULT);
            return state >= 0 && state < PROFILE_COUNT ? state : STATE_DEFAULT;
        }
        return getLegacyStateForPackage(packageName);
    }

    private int getLegacyStateForPackage(String packageName) {
        String value = mSharedPrefs.getString(THERMAL_CONTROL, null);
        if (value == null || value.isEmpty()) {
            return STATE_DEFAULT;
        }
        String[] modes = value.split(":");
        if (modes.length < 6) {
            return STATE_DEFAULT;
        }
        String token = packageName + ",";
        if (modes[0].contains(token)) return STATE_BENCHMARK;
        if (modes[1].contains(token)) return STATE_BROWSER;
        if (modes[2].contains(token)) return STATE_CAMERA;
        if (modes[3].contains(token)) return STATE_DIALER;
        if (modes[4].contains(token)) return STATE_GAMING;
        if (modes[5].contains(token)) return STATE_STREAMING;
        return STATE_DEFAULT;
    }

    protected void setDefaultThermalProfile() {
        FileUtils.writeLine(THERMAL_SCONFIG, mMapResolver.getDefaultCode());
    }

    protected void setThermalProfile(String packageName) {
        int state = getStateForPackage(packageName);
        FileUtils.writeLine(THERMAL_SCONFIG, mMapResolver.getCodeForState(state));
    }
}
