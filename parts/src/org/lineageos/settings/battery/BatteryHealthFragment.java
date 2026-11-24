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

package org.lineageos.settings.battery;

import android.content.Intent;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

import java.util.Locale;

public class BatteryHealthFragment extends SettingsBasePreferenceFragment {

    private static final long REFRESH_INTERVAL_MS = 2000L;

    private Preference mHealthPref;
    private Preference mCapacityPref;
    private Preference mCycleCountPref;
    private Preference mStatusPref;
    private Preference mTechPref;
    private Preference mCurrentPref;
    private Preference mPowerPref;
    private Preference mVoltagePref;
    private Preference mTempPref;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mUpdater = new Runnable() {
        @Override
        public void run() {
            updateStats();
            mHandler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.battery_health_settings, rootKey);

        mHealthPref = findPreference("battery_estimated_health");
        mCapacityPref = findPreference("battery_capacity");
        mCycleCountPref = findPreference("battery_cycle_count");
        mStatusPref = findPreference("battery_status");
        mTechPref = findPreference("battery_technology");
        mCurrentPref = findPreference("battery_current");
        mPowerPref = findPreference("battery_power");
        mVoltagePref = findPreference("battery_voltage");
        mTempPref = findPreference("battery_temp");
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.removeCallbacks(mUpdater);
        mHandler.post(mUpdater);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdater);
    }

    private void updateStats() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        String unavailable = getString(R.string.battery_health_unavailable);
        Intent battery = BatteryHealthUtils.getBatteryIntent(requireContext());

        int fullMah = BatteryHealthUtils.getChargeFullMah();
        int designMah = BatteryHealthUtils.getChargeFullDesignMah();

        if (mHealthPref != null) {
            if (fullMah > 0 && designMah > 0) {
                int pct = Math.min(100, Math.round((fullMah * 100.0f) / designMah));
                mHealthPref.setSummary(pct + "%");
            } else {
                mHealthPref.setSummary(unavailable);
            }
        }

        if (mCapacityPref != null) {
            if (fullMah > 0 && designMah > 0) {
                mCapacityPref.setSummary(fullMah + " / " + designMah + " mAh");
            } else if (designMah > 0) {
                mCapacityPref.setSummary(designMah + " mAh");
            } else {
                mCapacityPref.setSummary(unavailable);
            }
        }

        if (mCycleCountPref != null) {
            int cycles = BatteryHealthUtils.getCycleCount(battery);
            mCycleCountPref.setSummary(cycles >= 0 ? String.valueOf(cycles) : unavailable);
        }

        int status = battery != null
                ? battery.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                : BatteryManager.BATTERY_STATUS_UNKNOWN;
        int level = battery != null ? battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = battery != null ? battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100) : 100;
        int pctLevel = (level >= 0 && scale > 0) ? (level * 100 / scale) : -1;

        if (mStatusPref != null) {
            String statusLabel = switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING ->
                        getString(R.string.battery_health_status_charging);
                case BatteryManager.BATTERY_STATUS_DISCHARGING ->
                        getString(R.string.battery_health_status_discharging);
                case BatteryManager.BATTERY_STATUS_FULL ->
                        getString(R.string.battery_health_status_full);
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING ->
                        getString(R.string.battery_health_status_not_charging);
                default -> unavailable;
            };
            mStatusPref.setSummary(pctLevel >= 0 ? statusLabel + " (" + pctLevel + "%)" : statusLabel);
        }

        if (mTechPref != null) {
            String tech = battery != null ? battery.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) : null;
            mTechPref.setSummary(!TextUtils.isEmpty(tech) ? tech : unavailable);
        }

        int currentMa = BatteryHealthUtils.getCurrentMa(requireContext());
        float voltageV = BatteryHealthUtils.getVoltageVolts(battery);

        // Normalize sign: positive while charging, negative while discharging
        int displayCurrentMa = currentMa;
        if (currentMa != Integer.MIN_VALUE) {
            if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                displayCurrentMa = Math.abs(currentMa);
            } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                displayCurrentMa = -Math.abs(currentMa);
            }
        }

        if (mCurrentPref != null) {
            if (displayCurrentMa != Integer.MIN_VALUE) {
                String sign = displayCurrentMa > 0 ? "+" : "";
                mCurrentPref.setSummary(sign + displayCurrentMa + " mA");
            } else {
                mCurrentPref.setSummary(unavailable);
            }
        }

        if (mPowerPref != null) {
            if (displayCurrentMa != Integer.MIN_VALUE && voltageV > 0.0f) {
                float watts = (displayCurrentMa / 1000.0f) * voltageV;
                String sign = watts > 0.0f ? "+" : "";
                mPowerPref.setSummary(String.format(Locale.getDefault(), "%s%.2f W", sign, watts));
            } else {
                mPowerPref.setSummary(unavailable);
            }
        }

        if (mVoltagePref != null) {
            if (voltageV > 0.0f) {
                mVoltagePref.setSummary(String.format(Locale.getDefault(), "%.2f V", voltageV));
            } else {
                mVoltagePref.setSummary(unavailable);
            }
        }

        if (mTempPref != null) {
            int tempTenths = battery != null
                    ? battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) : -1;
            if (tempTenths > 0) {
                mTempPref.setSummary(
                        String.format(Locale.getDefault(), "%.1f °C", tempTenths / 10.0f));
            } else {
                mTempPref.setSummary(unavailable);
            }
        }
    }
}
