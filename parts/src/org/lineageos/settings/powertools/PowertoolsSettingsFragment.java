/*
 * Copyright (C) 2025 kenway214
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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class PowertoolsSettingsFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener,
                   Preference.OnPreferenceClickListener,
                   SharedPreferences.OnSharedPreferenceChangeListener {

    // General keys
    static final String KEY_POWER_PRESET = "power_preset";
    static final String KEY_APPLY_ON_BOOT = "apply_on_boot";
    static final String KEY_RESET_DEFAULTS = "reset_defaults";

    // GPU keys
    static final String KEY_GPU_ENABLE = "gpu_enable";
    static final String KEY_GPU_MIN_FREQ = "gpu_min_frequency";
    static final String KEY_GPU_MAX_FREQ = "gpu_max_frequency";
    static final String KEY_GPU_GOVERNOR = "gpu_governor";
    static final String KEY_GPU_ADRENO_IDLER = "gpu_adreno_idler";
    static final String KEY_GPU_IDLE_TIMER = "gpu_idle_timer";

    // CPU keys
    static final String KEY_CPU_ENABLE = "cpu_enable";
    static final String KEY_CPU_INPUT_BOOST = "cpu_input_boost";
    static final String KEY_CPU_WALT_PROFILE = "cpu_walt_profile";
    static final String KEY_CPU_LIMIT_PRIME = "cpu_limit_prime";
    static final String KEY_CPU_UCLAMP_PROFILE = "cpu_uclamp_profile";
    static final String KEY_CPU_LITTLE_MIN_FREQ = "cpu_little_min_frequency";
    static final String KEY_CPU_LITTLE_MAX_FREQ = "cpu_little_max_frequency";
    static final String KEY_CPU_LITTLE_GOVERNOR = "cpu_little_governor";
    static final String KEY_CPU_BIG_MIN_FREQ = "cpu_big_min_frequency";
    static final String KEY_CPU_BIG_MAX_FREQ = "cpu_big_max_frequency";
    static final String KEY_CPU_BIG_GOVERNOR = "cpu_big_governor";
    static final String KEY_CPU_PRIME_MIN_FREQ = "cpu_prime_min_frequency";
    static final String KEY_CPU_PRIME_MAX_FREQ = "cpu_prime_max_frequency";
    static final String KEY_CPU_PRIME_GOVERNOR = "cpu_prime_governor";

    private static final String PRESET_DEFAULT = "default";
    private static final String PRESET_BATTERY = "battery";
    private static final String PRESET_GAMING = "gaming";
    private static final String PRESET_CUSTOM = "custom";

    // General preferences
    private ListPreference mPowerPresetPref;
    private Preference mResetDefaultsPref;

    // GPU preferences
    private SwitchPreferenceCompat mGpuEnablePref;
    private ListPreference mGpuMinFreqPref;
    private ListPreference mGpuMaxFreqPref;
    private ListPreference mGpuGovernorPref;
    private SwitchPreferenceCompat mGpuAdrenoIdlerPref;
    private ListPreference mGpuIdleTimerPref;

    // CPU preferences
    private SwitchPreferenceCompat mCpuEnablePref;
    private ListPreference mCpuInputBoostPref;
    private ListPreference mCpuWaltProfilePref;
    private SwitchPreferenceCompat mCpuLimitPrimePref;
    private ListPreference mCpuUclampProfilePref;
    private ListPreference mCpuLittleMinFreqPref;
    private ListPreference mCpuLittleMaxFreqPref;
    private ListPreference mCpuLittleGovernorPref;
    private ListPreference mCpuBigMinFreqPref;
    private ListPreference mCpuBigMaxFreqPref;
    private ListPreference mCpuBigGovernorPref;
    private ListPreference mCpuPrimeMinFreqPref;
    private ListPreference mCpuPrimeMaxFreqPref;
    private ListPreference mCpuPrimeGovernorPref;

    // Dynamic defaults loaded strictly from sysfs
    private String mGpuDefaultMin;
    private String mGpuDefaultMax;
    private String mGpuDefaultGov;
    private String mCpuLittleDefaultMin;
    private String mCpuLittleDefaultMax;
    private String mCpuLittleDefaultGov;
    private String mCpuBigDefaultMin;
    private String mCpuBigDefaultMax;
    private String mCpuBigDefaultGov;
    private String mCpuPrimeDefaultMin;
    private String mCpuPrimeDefaultMax;
    private String mCpuPrimeDefaultGov;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.powertools_settings, rootKey);

        // General preferences
        mPowerPresetPref = findPreference(KEY_POWER_PRESET);
        if (mPowerPresetPref != null) {
            mPowerPresetPref.setOnPreferenceChangeListener(this);
        }
        mResetDefaultsPref = findPreference(KEY_RESET_DEFAULTS);
        if (mResetDefaultsPref != null) {
            mResetDefaultsPref.setOnPreferenceClickListener(this);
        }

        // GPU preferences
        mGpuEnablePref = findPreference(KEY_GPU_ENABLE);
        mGpuMinFreqPref = findPreference(KEY_GPU_MIN_FREQ);
        mGpuMaxFreqPref = findPreference(KEY_GPU_MAX_FREQ);
        mGpuGovernorPref = findPreference(KEY_GPU_GOVERNOR);
        mGpuAdrenoIdlerPref = findPreference(KEY_GPU_ADRENO_IDLER);
        mGpuIdleTimerPref = findPreference(KEY_GPU_IDLE_TIMER);
        if (mGpuEnablePref != null) {
            mGpuEnablePref.setOnPreferenceChangeListener(this);
        }
        setupGpuPreferences();

        // CPU preferences
        mCpuEnablePref = findPreference(KEY_CPU_ENABLE);
        if (mCpuEnablePref != null) {
            mCpuEnablePref.setOnPreferenceChangeListener(this);
        }
        mCpuInputBoostPref = findPreference(KEY_CPU_INPUT_BOOST);
        if (mCpuInputBoostPref != null) {
            mCpuInputBoostPref.setOnPreferenceChangeListener(this);
        }
        mCpuWaltProfilePref = findPreference(KEY_CPU_WALT_PROFILE);
        if (mCpuWaltProfilePref != null) {
            mCpuWaltProfilePref.setOnPreferenceChangeListener(this);
        }
        mCpuLimitPrimePref = findPreference(KEY_CPU_LIMIT_PRIME);
        if (mCpuLimitPrimePref != null) {
            mCpuLimitPrimePref.setOnPreferenceChangeListener(this);
        }
        mCpuUclampProfilePref = findPreference(KEY_CPU_UCLAMP_PROFILE);
        if (mCpuUclampProfilePref != null) {
            mCpuUclampProfilePref.setOnPreferenceChangeListener(this);
        }

        mCpuLittleMinFreqPref = findPreference(KEY_CPU_LITTLE_MIN_FREQ);
        mCpuLittleMaxFreqPref = findPreference(KEY_CPU_LITTLE_MAX_FREQ);
        mCpuLittleGovernorPref = findPreference(KEY_CPU_LITTLE_GOVERNOR);
        setupCpuClusterPreferences(CPUUtils.POLICY_LITTLE,
                mCpuLittleMinFreqPref, mCpuLittleMaxFreqPref, mCpuLittleGovernorPref,
                val -> mCpuLittleDefaultMin = val,
                val -> mCpuLittleDefaultMax = val,
                val -> mCpuLittleDefaultGov = val);

        mCpuBigMinFreqPref = findPreference(KEY_CPU_BIG_MIN_FREQ);
        mCpuBigMaxFreqPref = findPreference(KEY_CPU_BIG_MAX_FREQ);
        mCpuBigGovernorPref = findPreference(KEY_CPU_BIG_GOVERNOR);
        setupCpuClusterPreferences(CPUUtils.POLICY_BIG,
                mCpuBigMinFreqPref, mCpuBigMaxFreqPref, mCpuBigGovernorPref,
                val -> mCpuBigDefaultMin = val,
                val -> mCpuBigDefaultMax = val,
                val -> mCpuBigDefaultGov = val);

        mCpuPrimeMinFreqPref = findPreference(KEY_CPU_PRIME_MIN_FREQ);
        mCpuPrimeMaxFreqPref = findPreference(KEY_CPU_PRIME_MAX_FREQ);
        mCpuPrimeGovernorPref = findPreference(KEY_CPU_PRIME_GOVERNOR);
        setupCpuClusterPreferences(CPUUtils.POLICY_PRIME,
                mCpuPrimeMinFreqPref, mCpuPrimeMaxFreqPref, mCpuPrimeGovernorPref,
                val -> mCpuPrimeDefaultMin = val,
                val -> mCpuPrimeDefaultMax = val,
                val -> mCpuPrimeDefaultGov = val);
    }

    private interface StringConsumer {
        void accept(String value);
    }

    private void setupGpuPreferences() {
        String[] values = GPUUtils.getAvailableFrequencies();
        String[] entries = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            entries[i] = formatGpuFreq(Long.parseLong(values[i]));
        }
        if (values.length > 0) {
            mGpuDefaultMin = values[0];
            mGpuDefaultMax = values[values.length - 1];
        }

        String curMin = GPUUtils.getCurrentMinFrequency();
        String curMax = GPUUtils.getCurrentMaxFrequency();
        configureListPref(mGpuMinFreqPref, entries, values,
                !TextUtils.isEmpty(curMin) ? curMin : mGpuDefaultMin);
        configureListPref(mGpuMaxFreqPref, entries, values,
                !TextUtils.isEmpty(curMax) ? curMax : mGpuDefaultMax);

        String[] govValues = GPUUtils.getAvailableGovernors();
        String curGov = GPUUtils.getCurrentGovernor();
        mGpuDefaultGov = !TextUtils.isEmpty(curGov) ? curGov
                : (govValues.length > 0 ? govValues[0] : null);
        configureListPref(mGpuGovernorPref, govValues, govValues, mGpuDefaultGov);

        if (mGpuAdrenoIdlerPref != null) {
            boolean idlerAvailable = GPUUtils.isAdrenoIdlerAvailable();
            mGpuAdrenoIdlerPref.setVisible(idlerAvailable);
            if (idlerAvailable) {
                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(requireContext());
                if (!prefs.contains(KEY_GPU_ADRENO_IDLER)) {
                    mGpuAdrenoIdlerPref.setChecked(GPUUtils.isAdrenoIdlerEnabled());
                }
                mGpuAdrenoIdlerPref.setOnPreferenceChangeListener(this);
            }
        }

        if (mGpuIdleTimerPref != null) {
            mGpuIdleTimerPref.setOnPreferenceChangeListener(this);
            String curTimer = GPUUtils.getCurrentIdleTimer();
            if (TextUtils.isEmpty(mGpuIdleTimerPref.getValue())
                    && !TextUtils.isEmpty(curTimer)
                    && mGpuIdleTimerPref.findIndexOfValue(curTimer) >= 0) {
                mGpuIdleTimerPref.setValue(curTimer);
            }
        }
    }

    private void setupCpuClusterPreferences(int policyCpu,
            ListPreference minPref, ListPreference maxPref, ListPreference govPref,
            StringConsumer defaultMinOut, StringConsumer defaultMaxOut,
            StringConsumer defaultGovOut) {
        String[] freqValues = CPUUtils.getAvailableFrequencies(policyCpu);
        String[] freqEntries = new String[freqValues.length];
        for (int i = 0; i < freqValues.length; i++) {
            freqEntries[i] = formatCpuFreq(Long.parseLong(freqValues[i]));
        }
        String defMin = freqValues.length > 0 ? freqValues[0] : null;
        String defMax = freqValues.length > 0 ? freqValues[freqValues.length - 1] : null;
        if (defMin != null) {
            defaultMinOut.accept(defMin);
        }
        if (defMax != null) {
            defaultMaxOut.accept(defMax);
        }

        String curMin = CPUUtils.getCurrentMinFrequency(policyCpu);
        String curMax = CPUUtils.getCurrentMaxFrequency(policyCpu);
        configureListPref(minPref, freqEntries, freqValues,
                !TextUtils.isEmpty(curMin) ? curMin : defMin);
        configureListPref(maxPref, freqEntries, freqValues,
                !TextUtils.isEmpty(curMax) ? curMax : defMax);

        String[] govValues = CPUUtils.getAvailableGovernors(policyCpu);
        String curGov = CPUUtils.getCurrentGovernor(policyCpu);
        String defGov = !TextUtils.isEmpty(curGov) ? curGov
                : (govValues.length > 0 ? govValues[0] : null);
        if (defGov != null) {
            defaultGovOut.accept(defGov);
        }
        configureListPref(govPref, govValues, govValues, defGov);
    }

    private void configureListPref(ListPreference pref, String[] entries,
            String[] values, String fallbackValue) {
        if (pref == null) {
            return;
        }
        pref.setEntries(entries);
        pref.setEntryValues(values);
        pref.setOnPreferenceChangeListener(this);

        if (values.length == 0) {
            return;
        }
        String saved = pref.getValue();
        if (TextUtils.isEmpty(saved) || pref.findIndexOfValue(saved) < 0) {
            if (!TextUtils.isEmpty(fallbackValue) && pref.findIndexOfValue(fallbackValue) >= 0) {
                pref.setValue(fallbackValue);
            } else {
                pref.setValue(values[0]);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this);
        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updateUI() {
        boolean gpuEnabled = mGpuEnablePref != null && mGpuEnablePref.isChecked();
        setGpuPrefsEnabled(gpuEnabled);

        boolean cpuEnabled = mCpuEnablePref != null && mCpuEnablePref.isChecked();
        setCpuPrefsEnabled(cpuEnabled);
    }

    private void setGpuPrefsEnabled(boolean enabled) {
        setListPrefEnabled(mGpuMinFreqPref, enabled);
        setListPrefEnabled(mGpuMaxFreqPref, enabled);
        setListPrefEnabled(mGpuGovernorPref, enabled);
        if (mGpuAdrenoIdlerPref != null && mGpuAdrenoIdlerPref.isVisible()) {
            mGpuAdrenoIdlerPref.setEnabled(enabled);
        }
        if (mGpuIdleTimerPref != null) {
            mGpuIdleTimerPref.setEnabled(enabled);
        }
    }

    private void setCpuPrefsEnabled(boolean enabled) {
        if (mCpuInputBoostPref != null) {
            mCpuInputBoostPref.setEnabled(enabled);
        }
        if (mCpuWaltProfilePref != null) {
            mCpuWaltProfilePref.setEnabled(enabled);
        }
        if (mCpuLimitPrimePref != null) {
            mCpuLimitPrimePref.setEnabled(enabled);
        }
        if (mCpuUclampProfilePref != null) {
            mCpuUclampProfilePref.setEnabled(enabled);
        }

        boolean primeLimited = mCpuLimitPrimePref != null && mCpuLimitPrimePref.isChecked();
        setListPrefEnabled(mCpuLittleMinFreqPref, enabled);
        setListPrefEnabled(mCpuLittleMaxFreqPref, enabled);
        setListPrefEnabled(mCpuLittleGovernorPref, enabled);
        setListPrefEnabled(mCpuBigMinFreqPref, enabled);
        setListPrefEnabled(mCpuBigMaxFreqPref, enabled);
        setListPrefEnabled(mCpuBigGovernorPref, enabled);
        setListPrefEnabled(mCpuPrimeMinFreqPref, enabled);
        setListPrefEnabled(mCpuPrimeMaxFreqPref, enabled && !primeLimited);
        setListPrefEnabled(mCpuPrimeGovernorPref, enabled);
    }

    private void setListPrefEnabled(ListPreference pref, boolean enabled) {
        if (pref != null) {
            pref.setEnabled(enabled && pref.getEntryValues() != null
                    && pref.getEntryValues().length > 0);
        }
    }

    private void markCustomPreset() {
        if (mPowerPresetPref != null && !PRESET_CUSTOM.equals(mPowerPresetPref.getValue())) {
            mPowerPresetPref.setValue(PRESET_CUSTOM);
        }
    }

    private void restoreGpuDefaults() {
        if (!TextUtils.isEmpty(mGpuDefaultMin)) {
            GPUUtils.setGPUMinFrequency(mGpuDefaultMin);
            if (mGpuMinFreqPref != null) mGpuMinFreqPref.setValue(mGpuDefaultMin);
        }
        if (!TextUtils.isEmpty(mGpuDefaultMax)) {
            GPUUtils.setGPUMaxFrequency(mGpuDefaultMax);
            if (mGpuMaxFreqPref != null) mGpuMaxFreqPref.setValue(mGpuDefaultMax);
        }
        if (!TextUtils.isEmpty(mGpuDefaultGov)) {
            GPUUtils.setGPUGovernor(mGpuDefaultGov);
            if (mGpuGovernorPref != null) mGpuGovernorPref.setValue(mGpuDefaultGov);
        }
        if (GPUUtils.isAdrenoIdlerAvailable()) {
            GPUUtils.setAdrenoIdler(true);
            if (mGpuAdrenoIdlerPref != null) mGpuAdrenoIdlerPref.setChecked(true);
        }
        GPUUtils.setGPUIdleTimer("40");
        if (mGpuIdleTimerPref != null) mGpuIdleTimerPref.setValue("40");
    }

    private void restoreCpuDefaults() {
        CPUUtils.setInputBoost("default");
        if (mCpuInputBoostPref != null) mCpuInputBoostPref.setValue("default");

        CPUUtils.setWaltProfile("default");
        if (mCpuWaltProfilePref != null) mCpuWaltProfilePref.setValue("default");

        if (mCpuLimitPrimePref != null) mCpuLimitPrimePref.setChecked(false);

        CPUUtils.setUclampProfile("default");
        if (mCpuUclampProfilePref != null) mCpuUclampProfilePref.setValue("default");

        if (!TextUtils.isEmpty(mCpuLittleDefaultMin)
                && !TextUtils.isEmpty(mCpuLittleDefaultMax)
                && !TextUtils.isEmpty(mCpuLittleDefaultGov)) {
            CPUUtils.setCPULittleFreq(
                    mCpuLittleDefaultMin, mCpuLittleDefaultMax, mCpuLittleDefaultGov);
            if (mCpuLittleMinFreqPref != null) mCpuLittleMinFreqPref.setValue(mCpuLittleDefaultMin);
            if (mCpuLittleMaxFreqPref != null) mCpuLittleMaxFreqPref.setValue(mCpuLittleDefaultMax);
            if (mCpuLittleGovernorPref != null) mCpuLittleGovernorPref.setValue(mCpuLittleDefaultGov);
        }
        if (!TextUtils.isEmpty(mCpuBigDefaultMin)
                && !TextUtils.isEmpty(mCpuBigDefaultMax)
                && !TextUtils.isEmpty(mCpuBigDefaultGov)) {
            CPUUtils.setCPUBigFreq(mCpuBigDefaultMin, mCpuBigDefaultMax, mCpuBigDefaultGov);
            if (mCpuBigMinFreqPref != null) mCpuBigMinFreqPref.setValue(mCpuBigDefaultMin);
            if (mCpuBigMaxFreqPref != null) mCpuBigMaxFreqPref.setValue(mCpuBigDefaultMax);
            if (mCpuBigGovernorPref != null) mCpuBigGovernorPref.setValue(mCpuBigDefaultGov);
        }
        if (!TextUtils.isEmpty(mCpuPrimeDefaultMin)
                && !TextUtils.isEmpty(mCpuPrimeDefaultMax)
                && !TextUtils.isEmpty(mCpuPrimeDefaultGov)) {
            CPUUtils.setCPUPrimeFreq(mCpuPrimeDefaultMin, mCpuPrimeDefaultMax, mCpuPrimeDefaultGov);
            if (mCpuPrimeMinFreqPref != null) mCpuPrimeMinFreqPref.setValue(mCpuPrimeDefaultMin);
            if (mCpuPrimeMaxFreqPref != null) mCpuPrimeMaxFreqPref.setValue(mCpuPrimeDefaultMax);
            if (mCpuPrimeGovernorPref != null) mCpuPrimeGovernorPref.setValue(mCpuPrimeDefaultGov);
        }
    }

    private void applyPreset(String preset) {
        switch (preset) {
            case PRESET_DEFAULT -> {
                restoreGpuDefaults();
                restoreCpuDefaults();
                if (mGpuEnablePref != null) mGpuEnablePref.setChecked(false);
                if (mCpuEnablePref != null) mCpuEnablePref.setChecked(false);
                updateUI();
            }
            case PRESET_BATTERY -> {
                if (mGpuEnablePref != null) mGpuEnablePref.setChecked(true);
                if (mCpuEnablePref != null) mCpuEnablePref.setChecked(true);

                String gpuBattMax = GPUUtils.getBatteryMaxFrequency();
                if (!TextUtils.isEmpty(mGpuDefaultMin)) {
                    GPUUtils.setGPUMinFrequency(mGpuDefaultMin);
                    if (mGpuMinFreqPref != null) mGpuMinFreqPref.setValue(mGpuDefaultMin);
                }
                if (!TextUtils.isEmpty(gpuBattMax)) {
                    GPUUtils.setGPUMaxFrequency(gpuBattMax);
                    if (mGpuMaxFreqPref != null) mGpuMaxFreqPref.setValue(gpuBattMax);
                }
                if (!TextUtils.isEmpty(mGpuDefaultGov)) {
                    GPUUtils.setGPUGovernor(mGpuDefaultGov);
                    if (mGpuGovernorPref != null) mGpuGovernorPref.setValue(mGpuDefaultGov);
                }
                if (GPUUtils.isAdrenoIdlerAvailable()) {
                    GPUUtils.setAdrenoIdler(true);
                    if (mGpuAdrenoIdlerPref != null) mGpuAdrenoIdlerPref.setChecked(true);
                }
                GPUUtils.setGPUIdleTimer("40");
                if (mGpuIdleTimerPref != null) mGpuIdleTimerPref.setValue("40");

                CPUUtils.setInputBoost("low");
                if (mCpuInputBoostPref != null) mCpuInputBoostPref.setValue("low");
                CPUUtils.setWaltProfile("battery");
                if (mCpuWaltProfilePref != null) mCpuWaltProfilePref.setValue("battery");
                if (mCpuLimitPrimePref != null) mCpuLimitPrimePref.setChecked(true);
                CPUUtils.setUclampProfile("battery");
                if (mCpuUclampProfilePref != null) mCpuUclampProfilePref.setValue("battery");

                String bigBattMax = CPUUtils.getCappedMaxFrequency(CPUUtils.POLICY_BIG, 75);
                String primeBattMax = CPUUtils.getCappedMaxFrequency(CPUUtils.POLICY_PRIME, 55);
                if (!TextUtils.isEmpty(mCpuLittleDefaultMin)
                        && !TextUtils.isEmpty(mCpuLittleDefaultMax)) {
                    CPUUtils.setCPULittleFreq(
                            mCpuLittleDefaultMin, mCpuLittleDefaultMax, mCpuLittleDefaultGov);
                    if (mCpuLittleMinFreqPref != null) mCpuLittleMinFreqPref.setValue(mCpuLittleDefaultMin);
                    if (mCpuLittleMaxFreqPref != null) mCpuLittleMaxFreqPref.setValue(mCpuLittleDefaultMax);
                    if (mCpuLittleGovernorPref != null && !TextUtils.isEmpty(mCpuLittleDefaultGov)) {
                        mCpuLittleGovernorPref.setValue(mCpuLittleDefaultGov);
                    }
                }
                if (!TextUtils.isEmpty(mCpuBigDefaultMin) && !TextUtils.isEmpty(bigBattMax)) {
                    CPUUtils.setCPUBigFreq(mCpuBigDefaultMin, bigBattMax, mCpuBigDefaultGov);
                    if (mCpuBigMinFreqPref != null) mCpuBigMinFreqPref.setValue(mCpuBigDefaultMin);
                    if (mCpuBigMaxFreqPref != null) mCpuBigMaxFreqPref.setValue(bigBattMax);
                    if (mCpuBigGovernorPref != null && !TextUtils.isEmpty(mCpuBigDefaultGov)) {
                        mCpuBigGovernorPref.setValue(mCpuBigDefaultGov);
                    }
                }
                if (!TextUtils.isEmpty(mCpuPrimeDefaultMin) && !TextUtils.isEmpty(primeBattMax)) {
                    CPUUtils.setCPUPrimeFreq(mCpuPrimeDefaultMin, primeBattMax, mCpuPrimeDefaultGov);
                    if (mCpuPrimeMinFreqPref != null) mCpuPrimeMinFreqPref.setValue(mCpuPrimeDefaultMin);
                    if (mCpuPrimeMaxFreqPref != null) mCpuPrimeMaxFreqPref.setValue(primeBattMax);
                    if (mCpuPrimeGovernorPref != null && !TextUtils.isEmpty(mCpuPrimeDefaultGov)) {
                        mCpuPrimeGovernorPref.setValue(mCpuPrimeDefaultGov);
                    }
                }
                updateUI();
            }
            case PRESET_GAMING -> {
                if (mGpuEnablePref != null) mGpuEnablePref.setChecked(true);
                if (mCpuEnablePref != null) mCpuEnablePref.setChecked(true);

                if (!TextUtils.isEmpty(mGpuDefaultMin)) {
                    GPUUtils.setGPUMinFrequency(mGpuDefaultMin);
                    if (mGpuMinFreqPref != null) mGpuMinFreqPref.setValue(mGpuDefaultMin);
                }
                if (!TextUtils.isEmpty(mGpuDefaultMax)) {
                    GPUUtils.setGPUMaxFrequency(mGpuDefaultMax);
                    if (mGpuMaxFreqPref != null) mGpuMaxFreqPref.setValue(mGpuDefaultMax);
                }
                if (!TextUtils.isEmpty(mGpuDefaultGov)) {
                    GPUUtils.setGPUGovernor(mGpuDefaultGov);
                    if (mGpuGovernorPref != null) mGpuGovernorPref.setValue(mGpuDefaultGov);
                }
                if (GPUUtils.isAdrenoIdlerAvailable()) {
                    GPUUtils.setAdrenoIdler(false);
                    if (mGpuAdrenoIdlerPref != null) mGpuAdrenoIdlerPref.setChecked(false);
                }
                GPUUtils.setGPUIdleTimer("80");
                if (mGpuIdleTimerPref != null) mGpuIdleTimerPref.setValue("80");

                CPUUtils.setInputBoost("high");
                if (mCpuInputBoostPref != null) mCpuInputBoostPref.setValue("high");
                CPUUtils.setWaltProfile("responsive");
                if (mCpuWaltProfilePref != null) mCpuWaltProfilePref.setValue("responsive");
                if (mCpuLimitPrimePref != null) mCpuLimitPrimePref.setChecked(false);
                CPUUtils.setUclampProfile("performance");
                if (mCpuUclampProfilePref != null) mCpuUclampProfilePref.setValue("performance");

                if (!TextUtils.isEmpty(mCpuLittleDefaultMin)
                        && !TextUtils.isEmpty(mCpuLittleDefaultMax)) {
                    CPUUtils.setCPULittleFreq(
                            mCpuLittleDefaultMin, mCpuLittleDefaultMax, mCpuLittleDefaultGov);
                    if (mCpuLittleMinFreqPref != null) mCpuLittleMinFreqPref.setValue(mCpuLittleDefaultMin);
                    if (mCpuLittleMaxFreqPref != null) mCpuLittleMaxFreqPref.setValue(mCpuLittleDefaultMax);
                }
                if (!TextUtils.isEmpty(mCpuBigDefaultMin)
                        && !TextUtils.isEmpty(mCpuBigDefaultMax)) {
                    CPUUtils.setCPUBigFreq(
                            mCpuBigDefaultMin, mCpuBigDefaultMax, mCpuBigDefaultGov);
                    if (mCpuBigMinFreqPref != null) mCpuBigMinFreqPref.setValue(mCpuBigDefaultMin);
                    if (mCpuBigMaxFreqPref != null) mCpuBigMaxFreqPref.setValue(mCpuBigDefaultMax);
                }
                if (!TextUtils.isEmpty(mCpuPrimeDefaultMin)
                        && !TextUtils.isEmpty(mCpuPrimeDefaultMax)) {
                    CPUUtils.setCPUPrimeFreq(
                            mCpuPrimeDefaultMin, mCpuPrimeDefaultMax, mCpuPrimeDefaultGov);
                    if (mCpuPrimeMinFreqPref != null) mCpuPrimeMinFreqPref.setValue(mCpuPrimeDefaultMin);
                    if (mCpuPrimeMaxFreqPref != null) mCpuPrimeMaxFreqPref.setValue(mCpuPrimeDefaultMax);
                }
                updateUI();
            }
            default -> {
                // Custom preset: keep current manual settings untouched.
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mResetDefaultsPref) {
            restoreGpuDefaults();
            restoreCpuDefaults();
            if (mGpuEnablePref != null) mGpuEnablePref.setChecked(false);
            if (mCpuEnablePref != null) mCpuEnablePref.setChecked(false);
            if (mPowerPresetPref != null) mPowerPresetPref.setValue(PRESET_DEFAULT);
            updateUI();
            Toast.makeText(getActivity(),
                    R.string.powertools_reset_done, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPowerPresetPref) {
            applyPreset(newValue.toString());
            return true;
        }

        // GPU enable switch
        if (preference == mGpuEnablePref) {
            boolean enable = (Boolean) newValue;
            if (!enable) {
                restoreGpuDefaults();
            }
            setGpuPrefsEnabled(enable);
            markCustomPreset();
            return true;
        }

        if (preference == mGpuMinFreqPref) {
            String newMin = newValue.toString();
            long newMinVal = Long.parseLong(newMin);
            long curMaxVal = Long.parseLong(mGpuMaxFreqPref.getValue());
            if (newMinVal > curMaxVal) {
                Toast.makeText(getActivity(),
                        R.string.powertools_min_exceeds_max, Toast.LENGTH_SHORT).show();
                return false;
            }
            GPUUtils.setGPUMinFrequency(newMin);
            markCustomPreset();
            return true;
        }

        if (preference == mGpuMaxFreqPref) {
            String newMax = newValue.toString();
            long newMaxVal = Long.parseLong(newMax);
            long curMinVal = Long.parseLong(mGpuMinFreqPref.getValue());
            if (newMaxVal < curMinVal) {
                Toast.makeText(getActivity(),
                        R.string.powertools_max_below_min, Toast.LENGTH_SHORT).show();
                return false;
            }
            GPUUtils.setGPUMaxFrequency(newMax);
            markCustomPreset();
            return true;
        }

        if (preference == mGpuGovernorPref) {
            GPUUtils.setGPUGovernor(newValue.toString());
            markCustomPreset();
            return true;
        }

        if (preference == mGpuAdrenoIdlerPref) {
            GPUUtils.setAdrenoIdler((Boolean) newValue);
            markCustomPreset();
            return true;
        }

        if (preference == mGpuIdleTimerPref) {
            GPUUtils.setGPUIdleTimer(newValue.toString());
            markCustomPreset();
            return true;
        }

        // CPU enable switch
        if (preference == mCpuEnablePref) {
            boolean enable = (Boolean) newValue;
            if (!enable) {
                restoreCpuDefaults();
            }
            setCpuPrefsEnabled(enable);
            markCustomPreset();
            return true;
        }

        if (preference == mCpuInputBoostPref) {
            CPUUtils.setInputBoost(newValue.toString());
            markCustomPreset();
            return true;
        }

        if (preference == mCpuWaltProfilePref) {
            CPUUtils.setWaltProfile(newValue.toString());
            markCustomPreset();
            return true;
        }

        if (preference == mCpuLimitPrimePref) {
            boolean limit = (Boolean) newValue;
            String targetMax = limit
                    ? CPUUtils.getCappedMaxFrequency(CPUUtils.POLICY_PRIME, 55)
                    : mCpuPrimeDefaultMax;
            if (!TextUtils.isEmpty(targetMax) && mCpuPrimeMinFreqPref != null
                    && mCpuPrimeGovernorPref != null) {
                String minFreq = mCpuPrimeMinFreqPref.getValue();
                if (!TextUtils.isEmpty(minFreq)
                        && Long.parseLong(minFreq) > Long.parseLong(targetMax)) {
                    minFreq = mCpuPrimeDefaultMin;
                    if (!TextUtils.isEmpty(minFreq)) {
                        mCpuPrimeMinFreqPref.setValue(minFreq);
                    }
                }
                CPUUtils.setCPUPrimeFreq(minFreq, targetMax, mCpuPrimeGovernorPref.getValue());
                if (mCpuPrimeMaxFreqPref != null) {
                    mCpuPrimeMaxFreqPref.setValue(targetMax);
                    mCpuPrimeMaxFreqPref.setEnabled(!limit);
                }
            }
            markCustomPreset();
            return true;
        }

        if (preference == mCpuUclampProfilePref) {
            CPUUtils.setUclampProfile(newValue.toString());
            markCustomPreset();
            return true;
        }

        // CPU Little cluster changes
        if (preference == mCpuLittleMinFreqPref) {
            String newMin = newValue.toString();
            if (Long.parseLong(newMin) > Long.parseLong(mCpuLittleMaxFreqPref.getValue())) {
                Toast.makeText(getActivity(),
                        R.string.powertools_min_exceeds_max, Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPULittleFreq(newMin, mCpuLittleMaxFreqPref.getValue(),
                    mCpuLittleGovernorPref.getValue());
            markCustomPreset();
            return true;
        }
        if (preference == mCpuLittleMaxFreqPref) {
            String newMax = newValue.toString();
            if (Long.parseLong(newMax) < Long.parseLong(mCpuLittleMinFreqPref.getValue())) {
                Toast.makeText(getActivity(),
                        R.string.powertools_max_below_min, Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPULittleFreq(mCpuLittleMinFreqPref.getValue(), newMax,
                    mCpuLittleGovernorPref.getValue());
            markCustomPreset();
            return true;
        }
        if (preference == mCpuLittleGovernorPref) {
            CPUUtils.setCPULittleFreq(mCpuLittleMinFreqPref.getValue(),
                    mCpuLittleMaxFreqPref.getValue(), newValue.toString());
            markCustomPreset();
            return true;
        }

        // CPU Big cluster changes
        if (preference == mCpuBigMinFreqPref) {
            String newMin = newValue.toString();
            if (Long.parseLong(newMin) > Long.parseLong(mCpuBigMaxFreqPref.getValue())) {
                Toast.makeText(getActivity(),
                        R.string.powertools_min_exceeds_max, Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPUBigFreq(newMin, mCpuBigMaxFreqPref.getValue(),
                    mCpuBigGovernorPref.getValue());
            markCustomPreset();
            return true;
        }
        if (preference == mCpuBigMaxFreqPref) {
            String newMax = newValue.toString();
            if (Long.parseLong(newMax) < Long.parseLong(mCpuBigMinFreqPref.getValue())) {
                Toast.makeText(getActivity(),
                        R.string.powertools_max_below_min, Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPUBigFreq(mCpuBigMinFreqPref.getValue(), newMax,
                    mCpuBigGovernorPref.getValue());
            markCustomPreset();
            return true;
        }
        if (preference == mCpuBigGovernorPref) {
            CPUUtils.setCPUBigFreq(mCpuBigMinFreqPref.getValue(),
                    mCpuBigMaxFreqPref.getValue(), newValue.toString());
            markCustomPreset();
            return true;
        }

        // CPU Prime cluster changes
        if (preference == mCpuPrimeMinFreqPref) {
            String newMin = newValue.toString();
            if (Long.parseLong(newMin) > Long.parseLong(mCpuPrimeMaxFreqPref.getValue())) {
                Toast.makeText(getActivity(),
                        R.string.powertools_min_exceeds_max, Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPUPrimeFreq(newMin, mCpuPrimeMaxFreqPref.getValue(),
                    mCpuPrimeGovernorPref.getValue());
            markCustomPreset();
            return true;
        }
        if (preference == mCpuPrimeMaxFreqPref) {
            String newMax = newValue.toString();
            if (Long.parseLong(newMax) < Long.parseLong(mCpuPrimeMinFreqPref.getValue())) {
                Toast.makeText(getActivity(),
                        R.string.powertools_max_below_min, Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPUPrimeFreq(mCpuPrimeMinFreqPref.getValue(), newMax,
                    mCpuPrimeGovernorPref.getValue());
            markCustomPreset();
            return true;
        }
        if (preference == mCpuPrimeGovernorPref) {
            CPUUtils.setCPUPrimeFreq(mCpuPrimeMinFreqPref.getValue(),
                    mCpuPrimeMaxFreqPref.getValue(), newValue.toString());
            markCustomPreset();
            return true;
        }

        return true;
    }

    private String formatGpuFreq(long freqHz) {
        return (freqHz / 1_000_000) + "MHz";
    }

    private String formatCpuFreq(long freqKhz) {
        return (freqKhz / 1_000) + "MHz";
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateUI();
    }
}
