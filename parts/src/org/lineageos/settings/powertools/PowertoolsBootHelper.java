/*
 * Copyright (C) 2025 kenway214
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

package org.lineageos.settings.powertools;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.PreferenceManager;

public final class PowertoolsBootHelper {

    private static final String TAG = "PowertoolsBootHelper";

    private PowertoolsBootHelper() {}

    public static void applyOnBoot(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(PowertoolsSettingsFragment.KEY_APPLY_ON_BOOT, false)) {
            return;
        }

        Log.i(TAG, "Reapplying saved Powertools settings on boot");

        if (prefs.getBoolean(PowertoolsSettingsFragment.KEY_GPU_ENABLE, false)) {
            String gpuMin = prefs.getString(PowertoolsSettingsFragment.KEY_GPU_MIN_FREQ, null);
            String gpuMax = prefs.getString(PowertoolsSettingsFragment.KEY_GPU_MAX_FREQ, null);
            String gpuGov = prefs.getString(PowertoolsSettingsFragment.KEY_GPU_GOVERNOR, null);
            String idleTimer = prefs.getString(PowertoolsSettingsFragment.KEY_GPU_IDLE_TIMER, null);

            if (!TextUtils.isEmpty(gpuMin)) {
                GPUUtils.setGPUMinFrequency(gpuMin);
            }
            if (!TextUtils.isEmpty(gpuMax)) {
                GPUUtils.setGPUMaxFrequency(gpuMax);
            }
            if (!TextUtils.isEmpty(gpuGov)) {
                GPUUtils.setGPUGovernor(gpuGov);
            }
            if (GPUUtils.isAdrenoIdlerAvailable()) {
                GPUUtils.setAdrenoIdler(
                        prefs.getBoolean(PowertoolsSettingsFragment.KEY_GPU_ADRENO_IDLER, true));
            }
            if (!TextUtils.isEmpty(idleTimer)) {
                GPUUtils.setGPUIdleTimer(idleTimer);
            }
        }

        if (prefs.getBoolean(PowertoolsSettingsFragment.KEY_CPU_ENABLE, false)) {
            String inputBoost = prefs.getString(
                    PowertoolsSettingsFragment.KEY_CPU_INPUT_BOOST, null);
            String waltProfile = prefs.getString(
                    PowertoolsSettingsFragment.KEY_CPU_WALT_PROFILE, null);
            String uclampProfile = prefs.getString(
                    PowertoolsSettingsFragment.KEY_CPU_UCLAMP_PROFILE, null);

            if (!TextUtils.isEmpty(inputBoost)) {
                CPUUtils.setInputBoost(inputBoost);
            }
            if (!TextUtils.isEmpty(waltProfile)) {
                CPUUtils.setWaltProfile(waltProfile);
            }
            if (!TextUtils.isEmpty(uclampProfile)) {
                CPUUtils.setUclampProfile(uclampProfile);
            }

            CPUUtils.setCPULittleFreq(
                    prefs.getString(PowertoolsSettingsFragment.KEY_CPU_LITTLE_MIN_FREQ, null),
                    prefs.getString(PowertoolsSettingsFragment.KEY_CPU_LITTLE_MAX_FREQ, null),
                    prefs.getString(PowertoolsSettingsFragment.KEY_CPU_LITTLE_GOVERNOR, null));

            CPUUtils.setCPUBigFreq(
                    prefs.getString(PowertoolsSettingsFragment.KEY_CPU_BIG_MIN_FREQ, null),
                    prefs.getString(PowertoolsSettingsFragment.KEY_CPU_BIG_MAX_FREQ, null),
                    prefs.getString(PowertoolsSettingsFragment.KEY_CPU_BIG_GOVERNOR, null));

            String primeMax = prefs.getBoolean(
                    PowertoolsSettingsFragment.KEY_CPU_LIMIT_PRIME, false)
                    ? CPUUtils.getCappedMaxFrequency(CPUUtils.POLICY_PRIME, 55)
                    : prefs.getString(PowertoolsSettingsFragment.KEY_CPU_PRIME_MAX_FREQ, null);
            CPUUtils.setCPUPrimeFreq(
                    prefs.getString(PowertoolsSettingsFragment.KEY_CPU_PRIME_MIN_FREQ, null),
                    primeMax,
                    prefs.getString(PowertoolsSettingsFragment.KEY_CPU_PRIME_GOVERNOR, null));
        }

        if (prefs.getBoolean(PowertoolsSettingsFragment.KEY_ZRAM_ENABLE, false)) {
            String zramSize = prefs.getString(PowertoolsSettingsFragment.KEY_ZRAM_SIZE, null);
            String zramAlgo = prefs.getString(PowertoolsSettingsFragment.KEY_ZRAM_ALGO, null);
            if (!TextUtils.isEmpty(zramSize) || !TextUtils.isEmpty(zramAlgo)) {
                ZramUtils.applyZram(zramSize, zramAlgo);
            }
            ZramUtils.setSwappiness(
                    prefs.getString(PowertoolsSettingsFragment.KEY_VM_SWAPPINESS, null));
            ZramUtils.setVfsCachePressure(
                    prefs.getString(PowertoolsSettingsFragment.KEY_VM_VFS_PRESSURE, null));
            ZramUtils.setPageCluster(
                    prefs.getString(PowertoolsSettingsFragment.KEY_VM_PAGE_CLUSTER, null));
            ZramUtils.setDirtyProfile(
                    prefs.getString(PowertoolsSettingsFragment.KEY_VM_DIRTY_PROFILE, null));
        }
    }
}
