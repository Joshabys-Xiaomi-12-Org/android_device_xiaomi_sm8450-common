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

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.Locale;

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

    // Live monitor keys
    private static final String KEY_LIVE_CPU_CLOCKS = "live_cpu_clocks";
    private static final String KEY_LIVE_GPU_TEMP = "live_gpu_temp";
    private static final long LIVE_UPDATE_INTERVAL_MS = 1500L;

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

    // ZRAM & VM keys
    static final String KEY_ZRAM_ENABLE = "zram_enable";
    static final String KEY_ZRAM_SIZE = "zram_size";
    static final String KEY_ZRAM_ALGO = "zram_algorithm";
    static final String KEY_VM_SWAPPINESS = "vm_swappiness";
    static final String KEY_VM_VFS_PRESSURE = "vm_vfs_cache_pressure";
    static final String KEY_VM_PAGE_CLUSTER = "vm_page_cluster";
    static final String KEY_VM_DIRTY_PROFILE = "vm_dirty_profile";

    private static final String PRESET_DEFAULT = "default";
    private static final String PRESET_BATTERY = "battery";
    private static final String PRESET_GAMING = "gaming";
    private static final String PRESET_CUSTOM = "custom";

    // Live monitor
    private Preference mLiveCpuClocksPref;
    private Preference mLiveGpuTempPref;
    private final Handler mLiveHandler = new Handler(Looper.getMainLooper());
    private final Runnable mLiveUpdater = new Runnable() {
        @Override
        public void run() {
            updateLiveMonitor();
            mLiveHandler.postDelayed(this, LIVE_UPDATE_INTERVAL_MS);
        }
    };

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

    // ZRAM & VM preferences
    private SwitchPreferenceCompat mZramEnablePref;
    private ListPreference mZramSizePref;
    private ListPreference mZramAlgoPref;
    private ListPreference mVmSwappinessPref;
    private ListPreference mVmVfsPressurePref;
    private ListPreference mVmPageClusterPref;
    private ListPreference mVmDirtyProfilePref;

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
    private String mZramDefaultSize;
    private String mZramDefaultAlgo;
    private String mVmDefaultSwappiness;
    private String mVmDefaultVfsPressure;
    private String mVmDefaultPageCluster;
    private String mVmDefaultDirtyProfile;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.powertools_settings, rootKey);

        // Live monitor preferences
        mLiveCpuClocksPref = findPreference(KEY_LIVE_CPU_CLOCKS);
        mLiveGpuTempPref = findPreference(KEY_LIVE_GPU_TEMP);

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

        // ZRAM & VM preferences
        mZramEnablePref = findPreference(KEY_ZRAM_ENABLE);
        mZramSizePref = findPreference(KEY_ZRAM_SIZE);
        mZramAlgoPref = findPreference(KEY_ZRAM_ALGO);
        mVmSwappinessPref = findPreference(KEY_VM_SWAPPINESS);
        mVmVfsPressurePref = findPreference(KEY_VM_VFS_PRESSURE);
        mVmPageClusterPref = findPreference(KEY_VM_PAGE_CLUSTER);
        mVmDirtyProfilePref = findPreference(KEY_VM_DIRTY_PROFILE);
        if (mZramEnablePref != null) {
            mZramEnablePref.setOnPreferenceChangeListener(this);
        }
        setupZramPreferences();
    }

    private void setupZramPreferences() {
        mZramDefaultSize = ZramUtils.getCurrentSizeMb();
        if (TextUtils.isEmpty(mZramDefaultSize)) {
            mZramDefaultSize = "3584";
        }
        initStaticListPref(mZramSizePref, mZramDefaultSize);

        String[] algos = ZramUtils.getAvailableAlgorithms();
        String curAlgo = ZramUtils.getCurrentAlgorithm();
        mZramDefaultAlgo = !TextUtils.isEmpty(curAlgo) ? curAlgo
                : (algos.length > 0 ? algos[0] : null);
        configureListPref(mZramAlgoPref, algos, algos, mZramDefaultAlgo);

        mVmDefaultSwappiness = ZramUtils.getCurrentSwappiness();
        if (TextUtils.isEmpty(mVmDefaultSwappiness)) mVmDefaultSwappiness = "100";
        initStaticListPref(mVmSwappinessPref, mVmDefaultSwappiness);

        mVmDefaultVfsPressure = ZramUtils.getCurrentVfsCachePressure();
        if (TextUtils.isEmpty(mVmDefaultVfsPressure)) mVmDefaultVfsPressure = "50";
        initStaticListPref(mVmVfsPressurePref, mVmDefaultVfsPressure);

        mVmDefaultPageCluster = ZramUtils.getCurrentPageCluster();
        if (TextUtils.isEmpty(mVmDefaultPageCluster)) mVmDefaultPageCluster = "0";
        initStaticListPref(mVmPageClusterPref, mVmDefaultPageCluster);

        mVmDefaultDirtyProfile = ZramUtils.getCurrentDirtyProfile();
        initStaticListPref(mVmDirtyProfilePref, mVmDefaultDirtyProfile);
    }

    private void initStaticListPref(ListPreference pref, String fallbackValue) {
        if (pref == null) {
            return;
        }
        pref.setOnPreferenceChangeListener(this);
        String saved = pref.getValue();
        if (TextUtils.isEmpty(saved) || pref.findIndexOfValue(saved) < 0) {
            if (!TextUtils.isEmpty(fallbackValue) && pref.findIndexOfValue(fallbackValue) >= 0) {
                pref.setValue(fallbackValue);
            } else if (pref.getEntryValues() != null && pref.getEntryValues().length > 0) {
                pref.setValue(pref.getEntryValues()[0].toString());
            }
        }
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
        mLiveHandler.removeCallbacks(mLiveUpdater);
        mLiveHandler.post(mLiveUpdater);
    }

    @Override
    public void onPause() {
        super.onPause();
        mLiveHandler.removeCallbacks(mLiveUpdater);
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updateLiveMonitor() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        if (mLiveCpuClocksPref != null) {
            int little = CPUUtils.getCurrentFrequencyMhz(CPUUtils.POLICY_LITTLE);
            int big = CPUUtils.getCurrentFrequencyMhz(CPUUtils.POLICY_BIG);
            int prime = CPUUtils.getCurrentFrequencyMhz(CPUUtils.POLICY_PRIME);
            String lStr = little > 0 ? little + " MHz" : "--";
            String bStr = big > 0 ? big + " MHz" : "--";
            String pStr = prime > 0 ? prime + " MHz" : "--";
            mLiveCpuClocksPref.setSummary(
                    "Little: " + lStr + "  •  Big: " + bStr + "  •  Prime: " + pStr);
        }
        if (mLiveGpuTempPref != null) {
            int gpuMhz = GPUUtils.getCurrentClockMhz();
            String gpuStr = gpuMhz > 0 ? gpuMhz + " MHz" : "--";
            String tempStr = "--";
            Intent battery = requireContext().registerReceiver(
                    null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null) {
                int tempTenths = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                if (tempTenths > 0) {
                    tempStr = String.format(Locale.getDefault(), "%.1f °C", tempTenths / 10.0f);
                }
            }
            mLiveGpuTempPref.setSummary(
                    "GPU: " + gpuStr + "  •  " + getString(R.string.battery_health_temp_title)
                            + ": " + tempStr);
        }
    }

    private void updateUI() {
        boolean gpuEnabled = mGpuEnablePref != null && mGpuEnablePref.isChecked();
        setGpuPrefsEnabled(gpuEnabled);

        boolean cpuEnabled = mCpuEnablePref != null && mCpuEnablePref.isChecked();
        setCpuPrefsEnabled(cpuEnabled);

        boolean zramEnabled = mZramEnablePref != null && mZramEnablePref.isChecked();
        setZramPrefsEnabled(zramEnabled);
    }

    private void setZramPrefsEnabled(boolean enabled) {
        boolean zramSupported = ZramUtils.isZramSupported();
        if (mZramSizePref != null) {
            mZramSizePref.setEnabled(enabled && zramSupported);
        }
        setListPrefEnabled(mZramAlgoPref, enabled && zramSupported);
        if (mVmSwappinessPref != null) {
            mVmSwappinessPref.setEnabled(enabled);
        }
        if (mVmVfsPressurePref != null) {
            mVmVfsPressurePref.setEnabled(enabled);
        }
        if (mVmPageClusterPref != null) {
            mVmPageClusterPref.setEnabled(enabled);
        }
        if (mVmDirtyProfilePref != null) {
            mVmDirtyProfilePref.setEnabled(enabled);
        }
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

    private void restoreZramDefaults() {
        if (ZramUtils.isZramSupported()) {
            ZramUtils.applyZram(mZramDefaultSize, mZramDefaultAlgo);
            if (mZramSizePref != null && !TextUtils.isEmpty(mZramDefaultSize)
                    && mZramSizePref.findIndexOfValue(mZramDefaultSize) >= 0) {
                mZramSizePref.setValue(mZramDefaultSize);
            }
            if (mZramAlgoPref != null && !TextUtils.isEmpty(mZramDefaultAlgo)
                    && mZramAlgoPref.findIndexOfValue(mZramDefaultAlgo) >= 0) {
                mZramAlgoPref.setValue(mZramDefaultAlgo);
            }
        }
        if (!TextUtils.isEmpty(mVmDefaultSwappiness)) {
            ZramUtils.setSwappiness(mVmDefaultSwappiness);
            if (mVmSwappinessPref != null
                    && mVmSwappinessPref.findIndexOfValue(mVmDefaultSwappiness) >= 0) {
                mVmSwappinessPref.setValue(mVmDefaultSwappiness);
            }
        }
        if (!TextUtils.isEmpty(mVmDefaultVfsPressure)) {
            ZramUtils.setVfsCachePressure(mVmDefaultVfsPressure);
            if (mVmVfsPressurePref != null
                    && mVmVfsPressurePref.findIndexOfValue(mVmDefaultVfsPressure) >= 0) {
                mVmVfsPressurePref.setValue(mVmDefaultVfsPressure);
            }
        }
        if (!TextUtils.isEmpty(mVmDefaultPageCluster)) {
            ZramUtils.setPageCluster(mVmDefaultPageCluster);
            if (mVmPageClusterPref != null
                    && mVmPageClusterPref.findIndexOfValue(mVmDefaultPageCluster) >= 0) {
                mVmPageClusterPref.setValue(mVmDefaultPageCluster);
            }
        }
        if (!TextUtils.isEmpty(mVmDefaultDirtyProfile)) {
            ZramUtils.setDirtyProfile(mVmDefaultDirtyProfile);
            if (mVmDirtyProfilePref != null
                    && mVmDirtyProfilePref.findIndexOfValue(mVmDefaultDirtyProfile) >= 0) {
                mVmDirtyProfilePref.setValue(mVmDefaultDirtyProfile);
            }
        }
    }

    private void applyPreset(String preset) {
        switch (preset) {
            case PRESET_DEFAULT -> {
                restoreGpuDefaults();
                restoreCpuDefaults();
                restoreZramDefaults();
                if (mGpuEnablePref != null) mGpuEnablePref.setChecked(false);
                if (mCpuEnablePref != null) mCpuEnablePref.setChecked(false);
                if (mZramEnablePref != null) mZramEnablePref.setChecked(false);
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
            restoreZramDefaults();
            if (mGpuEnablePref != null) mGpuEnablePref.setChecked(false);
            if (mCpuEnablePref != null) mCpuEnablePref.setChecked(false);
            if (mZramEnablePref != null) mZramEnablePref.setChecked(false);
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

        // ZRAM & VM changes
        if (preference == mZramEnablePref) {
            boolean enable = (Boolean) newValue;
            if (!enable) {
                restoreZramDefaults();
            }
            setZramPrefsEnabled(enable);
            markCustomPreset();
            return true;
        }
        if (preference == mZramSizePref) {
            String newSize = newValue.toString();
            String algo = mZramAlgoPref != null ? mZramAlgoPref.getValue() : mZramDefaultAlgo;
            ZramUtils.applyZram(newSize, algo);
            markCustomPreset();
            return true;
        }
        if (preference == mZramAlgoPref) {
            String newAlgo = newValue.toString();
            String size = mZramSizePref != null ? mZramSizePref.getValue() : mZramDefaultSize;
            ZramUtils.applyZram(size, newAlgo);
            markCustomPreset();
            return true;
        }
        if (preference == mVmSwappinessPref) {
            ZramUtils.setSwappiness(newValue.toString());
            markCustomPreset();
            return true;
        }
        if (preference == mVmVfsPressurePref) {
            ZramUtils.setVfsCachePressure(newValue.toString());
            markCustomPreset();
            return true;
        }
        if (preference == mVmPageClusterPref) {
            ZramUtils.setPageCluster(newValue.toString());
            markCustomPreset();
            return true;
        }
        if (preference == mVmDirtyProfilePref) {
            ZramUtils.setDirtyProfile(newValue.toString());
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
