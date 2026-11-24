/*
 * Copyright (C) 2026 The LineageOS Project
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

package org.lineageos.settings.refreshrate;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.Display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RefreshUtils {

    public static final String PREFS_NAME = "per_app_refresh_rate";
    public static final String KEY_ENABLED = "per_app_refresh_enabled";
    private static final String KEY_APP_PREFIX = "app_rate_";
    private static final String KEY_SAVED_MIN_RATE = "saved_system_min_rate";
    private static final String KEY_SAVED_PEAK_RATE = "saved_system_peak_rate";
    private static final String KEY_OVERRIDE_ACTIVE = "override_active";

    private static final String SETTING_MIN_REFRESH_RATE = "min_refresh_rate";
    private static final String SETTING_PEAK_REFRESH_RATE = "peak_refresh_rate";

    public static final int RATE_DEFAULT = 0;

    private final Context mContext;
    private final SharedPreferences mPrefs;

    public RefreshUtils(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, RefreshService.class), UserHandle.CURRENT);
    }

    public boolean isEnabled() {
        return mPrefs.getBoolean(KEY_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (!enabled) {
            restoreSystemRefreshRate();
        }
    }

    public int getRateForPackage(String packageName) {
        return mPrefs.getInt(KEY_APP_PREFIX + packageName, RATE_DEFAULT);
    }

    public void setRateForPackage(String packageName, int rateHz) {
        SharedPreferences.Editor editor = mPrefs.edit();
        if (rateHz <= 0) {
            editor.remove(KEY_APP_PREFIX + packageName);
        } else {
            editor.putInt(KEY_APP_PREFIX + packageName, rateHz);
        }
        editor.apply();
    }

    public void applyForPackage(String packageName) {
        if (!isEnabled()) {
            restoreSystemRefreshRate();
            return;
        }
        int rate = getRateForPackage(packageName);
        if (rate > 0) {
            applyOverrideRate(rate);
        } else {
            restoreSystemRefreshRate();
        }
    }

    public void restoreSystemRefreshRate() {
        if (!mPrefs.getBoolean(KEY_OVERRIDE_ACTIVE, false)) {
            return;
        }
        ContentResolver cr = mContext.getContentResolver();
        float savedMin = mPrefs.getFloat(KEY_SAVED_MIN_RATE, -1.0f);
        float savedPeak = mPrefs.getFloat(KEY_SAVED_PEAK_RATE, -1.0f);

        if (savedMin > 0.0f) {
            Settings.System.putFloat(cr, SETTING_MIN_REFRESH_RATE, savedMin);
        } else {
            Settings.System.putString(cr, SETTING_MIN_REFRESH_RATE, null);
        }
        if (savedPeak > 0.0f) {
            Settings.System.putFloat(cr, SETTING_PEAK_REFRESH_RATE, savedPeak);
        } else {
            Settings.System.putString(cr, SETTING_PEAK_REFRESH_RATE, null);
        }
        mPrefs.edit().putBoolean(KEY_OVERRIDE_ACTIVE, false).apply();
    }

    private void applyOverrideRate(int rateHz) {
        ContentResolver cr = mContext.getContentResolver();
        if (!mPrefs.getBoolean(KEY_OVERRIDE_ACTIVE, false)) {
            float curMin = Settings.System.getFloat(cr, SETTING_MIN_REFRESH_RATE, -1.0f);
            float curPeak = Settings.System.getFloat(cr, SETTING_PEAK_REFRESH_RATE, -1.0f);
            mPrefs.edit()
                    .putFloat(KEY_SAVED_MIN_RATE, curMin)
                    .putFloat(KEY_SAVED_PEAK_RATE, curPeak)
                    .putBoolean(KEY_OVERRIDE_ACTIVE, true)
                    .apply();
        }
        Settings.System.putFloat(cr, SETTING_MIN_REFRESH_RATE, (float) rateHz);
        Settings.System.putFloat(cr, SETTING_PEAK_REFRESH_RATE, (float) rateHz);
    }

    public static int[] getSupportedRefreshRates(Context context) {
        List<Integer> rates = new ArrayList<>();
        DisplayManager dm = context.getSystemService(DisplayManager.class);
        if (dm != null) {
            Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);
            if (display != null) {
                for (Display.Mode mode : display.getSupportedModes()) {
                    int hz = Math.round(mode.getRefreshRate());
                    if (hz >= 30 && !rates.contains(hz)) {
                        rates.add(hz);
                    }
                }
            }
        }
        if (rates.isEmpty()) {
            rates.add(60);
            rates.add(90);
            rates.add(120);
        }
        Collections.sort(rates);
        int[] result = new int[rates.size()];
        for (int i = 0; i < rates.size(); i++) {
            result[i] = rates.get(i);
        }
        return result;
    }
}
