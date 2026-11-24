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
                   SharedPreferences.OnSharedPreferenceChangeListener {

    // Power Profile
    private static final String KEY_POWER_PROFILE_MODE = "power_profile_mode";
    private ListPreference mPowerProfilePreference;
    private PowerProfileUtil mPowerProfileUtil;

    // GPU settings keys
    private static final String KEY_GPU_ENABLE = "gpu_enable";
    private static final String KEY_GPU_MIN_FREQ = "gpu_min_frequency";
    private static final String KEY_GPU_MAX_FREQ = "gpu_max_frequency";
    private SwitchPreferenceCompat mGpuEnablePref;
    private ListPreference mGpuMinFreqPref;
    private ListPreference mGpuMaxFreqPref;

    // CPU settings keys
    private static final String KEY_CPU_ENABLE = "cpu_enable";
    private static final String KEY_CPU_LITTLE_MIN_FREQ = "cpu_little_min_frequency";
    private static final String KEY_CPU_LITTLE_MAX_FREQ = "cpu_little_max_frequency";
    private static final String KEY_CPU_LITTLE_GOVERNOR = "cpu_little_governor";
    private static final String KEY_CPU_BIG_MIN_FREQ = "cpu_big_min_frequency";
    private static final String KEY_CPU_BIG_MAX_FREQ = "cpu_big_max_frequency";
    private static final String KEY_CPU_BIG_GOVERNOR = "cpu_big_governor";
    private static final String KEY_CPU_PRIME_MIN_FREQ = "cpu_prime_min_frequency";
    private static final String KEY_CPU_PRIME_MAX_FREQ = "cpu_prime_max_frequency";
    private static final String KEY_CPU_PRIME_GOVERNOR = "cpu_prime_governor";

    private SwitchPreferenceCompat mCpuEnablePref;
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

        mPowerProfilePreference = findPreference(KEY_POWER_PROFILE_MODE);
        mPowerProfileUtil = new PowerProfileUtil(requireContext());
        if (mPowerProfilePreference != null) {
            int currentMode = mPowerProfileUtil.getManagedMode();
            mPowerProfilePreference.setValue(String.valueOf(currentMode));
            mPowerProfilePreference.setSummary(mPowerProfileUtil.getModeLabel());
            mPowerProfilePreference.setOnPreferenceChangeListener(this);
        }

        // GPU preferences
        mGpuEnablePref = findPreference(KEY_GPU_ENABLE);
        mGpuMinFreqPref = findPreference(KEY_GPU_MIN_FREQ);
        mGpuMaxFreqPref = findPreference(KEY_GPU_MAX_FREQ);
        if (mGpuEnablePref != null) {
            mGpuEnablePref.setOnPreferenceChangeListener(this);
        }
        setupGpuPreferences();

        // CPU preferences
        mCpuEnablePref = findPreference(KEY_CPU_ENABLE);
        if (mCpuEnablePref != null) {
            mCpuEnablePref.setOnPreferenceChangeListener(this);
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
        if (mPowerProfilePreference != null) {
            boolean masterEnabled = mPowerProfileUtil.isMasterEnabled();
            if (masterEnabled) {
                mPowerProfilePreference.setEnabled(false);
                mPowerProfilePreference.setSummary(
                        getString(R.string.powerprofile_tile_disabled_subtitle));
            } else {
                mPowerProfilePreference.setEnabled(true);
                mPowerProfilePreference.setSummary(mPowerProfileUtil.getModeLabel());
            }
        }
        // GPU section
        boolean gpuEnabled = mGpuEnablePref != null && mGpuEnablePref.isChecked();
        setGpuPrefsEnabled(gpuEnabled);

        // CPU section
        boolean cpuEnabled = mCpuEnablePref != null && mCpuEnablePref.isChecked();
        setCpuPrefsEnabled(cpuEnabled);
    }

    private void setGpuPrefsEnabled(boolean enabled) {
        if (mGpuMinFreqPref != null) {
            mGpuMinFreqPref.setEnabled(enabled && mGpuMinFreqPref.getEntryValues() != null
                    && mGpuMinFreqPref.getEntryValues().length > 0);
        }
        if (mGpuMaxFreqPref != null) {
            mGpuMaxFreqPref.setEnabled(enabled && mGpuMaxFreqPref.getEntryValues() != null
                    && mGpuMaxFreqPref.getEntryValues().length > 0);
        }
    }

    private void setCpuPrefsEnabled(boolean enabled) {
        ListPreference[] cpuPrefs = {
                mCpuLittleMinFreqPref, mCpuLittleMaxFreqPref, mCpuLittleGovernorPref,
                mCpuBigMinFreqPref, mCpuBigMaxFreqPref, mCpuBigGovernorPref,
                mCpuPrimeMinFreqPref, mCpuPrimeMaxFreqPref, mCpuPrimeGovernorPref,
        };
        for (ListPreference pref : cpuPrefs) {
            if (pref != null) {
                pref.setEnabled(enabled && pref.getEntryValues() != null
                        && pref.getEntryValues().length > 0);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Power Profile mode change
        if (KEY_POWER_PROFILE_MODE.equals(preference.getKey())) {
            int mode = Integer.parseInt((String) newValue);
            mPowerProfileUtil.setMode(mode);
            mPowerProfilePreference.setSummary(mPowerProfileUtil.getModeLabel());
            Toast.makeText(getActivity(),
                    "Applied successfully power profile to " + mPowerProfileUtil.getModeLabel(),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        // GPU enable switch
        if (preference == mGpuEnablePref) {
            boolean enable = (Boolean) newValue;
            if (!enable) {
                if (!TextUtils.isEmpty(mGpuDefaultMin)) {
                    GPUUtils.setGPUMinFrequency(mGpuDefaultMin);
                    if (mGpuMinFreqPref != null) mGpuMinFreqPref.setValue(mGpuDefaultMin);
                }
                if (!TextUtils.isEmpty(mGpuDefaultMax)) {
                    GPUUtils.setGPUMaxFrequency(mGpuDefaultMax);
                    if (mGpuMaxFreqPref != null) mGpuMaxFreqPref.setValue(mGpuDefaultMax);
                }
                Toast.makeText(getActivity(),
                        "GPU disabled, defaults restored",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(),
                        "GPU optimizer enabled",
                        Toast.LENGTH_SHORT).show();
            }
            setGpuPrefsEnabled(enable);
            return true;
        }

        // GPU min frequency change
        if (preference == mGpuMinFreqPref) {
            String newMin = newValue.toString();
            long newMinVal = Long.parseLong(newMin);
            long curMaxVal = Long.parseLong(mGpuMaxFreqPref.getValue());
            if (newMinVal > curMaxVal) {
                Toast.makeText(getActivity(),
                        "Failed: GPU min frequency cannot exceed max frequency",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            GPUUtils.setGPUMinFrequency(newMin);
            Toast.makeText(getActivity(),
                    "GPU min frequency -> " + formatGpuFreq(newMinVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        // GPU max frequency change
        if (preference == mGpuMaxFreqPref) {
            String newMax = newValue.toString();
            long newMaxVal = Long.parseLong(newMax);
            long curMinVal = Long.parseLong(mGpuMinFreqPref.getValue());
            if (newMaxVal < curMinVal) {
                Toast.makeText(getActivity(),
                        "Failed: GPU max frequency cannot be lower than min frequency",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            GPUUtils.setGPUMaxFrequency(newMax);
            Toast.makeText(getActivity(),
                    "GPU max frequency -> " + formatGpuFreq(newMaxVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        // CPU enable switch
        if (preference == mCpuEnablePref) {
            boolean enable = (Boolean) newValue;
            if (!enable) {
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
                    CPUUtils.setCPUBigFreq(
                            mCpuBigDefaultMin, mCpuBigDefaultMax, mCpuBigDefaultGov);
                    if (mCpuBigMinFreqPref != null) mCpuBigMinFreqPref.setValue(mCpuBigDefaultMin);
                    if (mCpuBigMaxFreqPref != null) mCpuBigMaxFreqPref.setValue(mCpuBigDefaultMax);
                    if (mCpuBigGovernorPref != null) mCpuBigGovernorPref.setValue(mCpuBigDefaultGov);
                }
                if (!TextUtils.isEmpty(mCpuPrimeDefaultMin)
                        && !TextUtils.isEmpty(mCpuPrimeDefaultMax)
                        && !TextUtils.isEmpty(mCpuPrimeDefaultGov)) {
                    CPUUtils.setCPUPrimeFreq(
                            mCpuPrimeDefaultMin, mCpuPrimeDefaultMax, mCpuPrimeDefaultGov);
                    if (mCpuPrimeMinFreqPref != null) mCpuPrimeMinFreqPref.setValue(mCpuPrimeDefaultMin);
                    if (mCpuPrimeMaxFreqPref != null) mCpuPrimeMaxFreqPref.setValue(mCpuPrimeDefaultMax);
                    if (mCpuPrimeGovernorPref != null) mCpuPrimeGovernorPref.setValue(mCpuPrimeDefaultGov);
                }
                Toast.makeText(getActivity(),
                        "CPU optimizer disabled, defaults restored",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(),
                        "CPU optimizer enabled",
                        Toast.LENGTH_SHORT).show();
            }
            setCpuPrefsEnabled(enable);
            return true;
        }

        // CPU Little cluster changes
        if (preference == mCpuLittleMinFreqPref) {
            String newMin = newValue.toString();
            long newMinVal = Long.parseLong(newMin);
            long curMaxVal = Long.parseLong(mCpuLittleMaxFreqPref.getValue());
            if (newMinVal > curMaxVal) {
                Toast.makeText(getActivity(),
                        "Failed: CPU little min frequency cannot exceed max",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPULittleFreq(newMin, mCpuLittleMaxFreqPref.getValue(),
                    mCpuLittleGovernorPref.getValue());
            Toast.makeText(getActivity(),
                    "CPU little min frequency -> " + formatCpuFreq(newMinVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference == mCpuLittleMaxFreqPref) {
            String newMax = newValue.toString();
            long newMaxVal = Long.parseLong(newMax);
            long curMinVal = Long.parseLong(mCpuLittleMinFreqPref.getValue());
            if (newMaxVal < curMinVal) {
                Toast.makeText(getActivity(),
                        "Failed: CPU little max frequency cannot be lower than min",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPULittleFreq(mCpuLittleMinFreqPref.getValue(), newMax,
                    mCpuLittleGovernorPref.getValue());
            Toast.makeText(getActivity(),
                    "CPU little max frequency -> " + formatCpuFreq(newMaxVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference == mCpuLittleGovernorPref) {
            CPUUtils.setCPULittleFreq(mCpuLittleMinFreqPref.getValue(),
                    mCpuLittleMaxFreqPref.getValue(), newValue.toString());
            Toast.makeText(getActivity(),
                    "CPU little governor -> " + newValue.toString(),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        // CPU Big cluster changes
        if (preference == mCpuBigMinFreqPref) {
            String newMin = newValue.toString();
            long newMinVal = Long.parseLong(newMin);
            long curMaxVal = Long.parseLong(mCpuBigMaxFreqPref.getValue());
            if (newMinVal > curMaxVal) {
                Toast.makeText(getActivity(),
                        "Failed: CPU big min frequency cannot exceed max",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPUBigFreq(newMin, mCpuBigMaxFreqPref.getValue(),
                    mCpuBigGovernorPref.getValue());
            Toast.makeText(getActivity(),
                    "CPU big min frequency -> " + formatCpuFreq(newMinVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference == mCpuBigMaxFreqPref) {
            String newMax = newValue.toString();
            long newMaxVal = Long.parseLong(newMax);
            long curMinVal = Long.parseLong(mCpuBigMinFreqPref.getValue());
            if (newMaxVal < curMinVal) {
                Toast.makeText(getActivity(),
                        "Failed: CPU big max frequency cannot be lower than min",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPUBigFreq(mCpuBigMinFreqPref.getValue(), newMax,
                    mCpuBigGovernorPref.getValue());
            Toast.makeText(getActivity(),
                    "CPU big max frequency -> " + formatCpuFreq(newMaxVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference == mCpuBigGovernorPref) {
            CPUUtils.setCPUBigFreq(mCpuBigMinFreqPref.getValue(),
                    mCpuBigMaxFreqPref.getValue(), newValue.toString());
            Toast.makeText(getActivity(),
                    "CPU big governor -> " + newValue.toString(),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        // CPU Prime cluster changes
        if (preference == mCpuPrimeMinFreqPref) {
            String newMin = newValue.toString();
            long newMinVal = Long.parseLong(newMin);
            long curMaxVal = Long.parseLong(mCpuPrimeMaxFreqPref.getValue());
            if (newMinVal > curMaxVal) {
                Toast.makeText(getActivity(),
                        "Failed: CPU prime min frequency cannot exceed max",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPUPrimeFreq(newMin, mCpuPrimeMaxFreqPref.getValue(),
                    mCpuPrimeGovernorPref.getValue());
            Toast.makeText(getActivity(),
                    "CPU prime min frequency -> " + formatCpuFreq(newMinVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference == mCpuPrimeMaxFreqPref) {
            String newMax = newValue.toString();
            long newMaxVal = Long.parseLong(newMax);
            long curMinVal = Long.parseLong(mCpuPrimeMinFreqPref.getValue());
            if (newMaxVal < curMinVal) {
                Toast.makeText(getActivity(),
                        "Failed: CPU prime max frequency cannot be lower than min",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            CPUUtils.setCPUPrimeFreq(mCpuPrimeMinFreqPref.getValue(), newMax,
                    mCpuPrimeGovernorPref.getValue());
            Toast.makeText(getActivity(),
                    "CPU prime max frequency -> " + formatCpuFreq(newMaxVal),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        if (preference == mCpuPrimeGovernorPref) {
            CPUUtils.setCPUPrimeFreq(mCpuPrimeMinFreqPref.getValue(),
                    mCpuPrimeMaxFreqPref.getValue(), newValue.toString());
            Toast.makeText(getActivity(),
                    "CPU prime governor -> " + newValue.toString(),
                    Toast.LENGTH_SHORT).show();
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
