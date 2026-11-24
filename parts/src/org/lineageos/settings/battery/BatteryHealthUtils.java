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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.text.TextUtils;

import org.lineageos.settings.utils.FileUtils;

public final class BatteryHealthUtils {

    private static final String PATH_CYCLE_COUNT = "/sys/class/power_supply/battery/cycle_count";
    private static final String PATH_CHARGE_FULL = "/sys/class/power_supply/battery/charge_full";
    private static final String PATH_CHARGE_FULL_DESIGN =
            "/sys/class/power_supply/battery/charge_full_design";
    private static final String PATH_CURRENT_NOW = "/sys/class/power_supply/battery/current_now";
    private static final String PATH_VOLTAGE_NOW = "/sys/class/power_supply/battery/voltage_now";

    private BatteryHealthUtils() {}

    public static long readLong(String path) {
        String raw = FileUtils.readOneLine(path);
        if (TextUtils.isEmpty(raw)) {
            return Long.MIN_VALUE;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return Long.MIN_VALUE;
        }
    }

    public static int getChargeFullMah() {
        long raw = readLong(PATH_CHARGE_FULL);
        if (raw <= 0) {
            return -1;
        }
        return raw > 100_000L ? (int) (raw / 1_000L) : (int) raw;
    }

    public static int getChargeFullDesignMah() {
        long raw = readLong(PATH_CHARGE_FULL_DESIGN);
        if (raw <= 0) {
            return -1;
        }
        return raw > 100_000L ? (int) (raw / 1_000L) : (int) raw;
    }

    public static int getCycleCount(Intent batteryIntent) {
        long sysfs = readLong(PATH_CYCLE_COUNT);
        if (sysfs >= 0) {
            return (int) sysfs;
        }
        if (batteryIntent != null) {
            int fromIntent = batteryIntent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1);
            if (fromIntent >= 0) {
                return fromIntent;
            }
        }
        return -1;
    }

    public static int getCurrentMa(Context context) {
        BatteryManager bm = context.getSystemService(BatteryManager.class);
        if (bm != null) {
            int ua = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            if (ua != Integer.MIN_VALUE && ua != 0) {
                return Math.abs(ua) > 10_000 ? ua / 1_000 : ua;
            }
        }
        long raw = readLong(PATH_CURRENT_NOW);
        if (raw == Long.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return Math.abs(raw) > 10_000L ? (int) (raw / 1_000L) : (int) raw;
    }

    public static float getVoltageVolts(Intent batteryIntent) {
        if (batteryIntent != null) {
            int mv = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            if (mv > 0) {
                return mv / 1000.0f;
            }
        }
        long uv = readLong(PATH_VOLTAGE_NOW);
        if (uv > 0) {
            return uv > 100_000L ? uv / 1_000_000.0f : uv / 1_000.0f;
        }
        return -1.0f;
    }

    public static Intent getBatteryIntent(Context context) {
        return context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }
}
