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

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

import java.util.Locale;

public class PowertoolsSettingsFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener,
                   Preference.OnPreferenceClickListener,
                   SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int TAB_GENERAL = 0;
    public static final int TAB_CPU = 1;
    public static final int TAB_GPU = 2;
    public static final int TAB_ZRAM = 3;
    public static final int TAB_IO_NET = 4;

    private static final String ARG_TAB = "arg_tab";

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

    // I/O & Network keys
    static final String KEY_IO_NET_ENABLE = "io_net_enable";
    static final String KEY_IO_SCHEDULER = "io_scheduler";
    static final String KEY_IO_READ_AHEAD = "io_read_ahead";
    static final String KEY_NET_TCP_CONGESTION = "net_tcp_congestion";

    // Persisted stock ZRAM/VM/IO/Net defaults
    private static final String KEY_STOCK_ZRAM_SIZE = "stock_zram_size";
    private static final String KEY_STOCK_ZRAM_ALGO = "stock_zram_algo";
    private static final String KEY_STOCK_VM_SWAPPINESS = "stock_vm_swappiness";
    private static final String KEY_STOCK_VM_VFS_PRESSURE = "stock_vm_vfs_pressure";
    private static final String KEY_STOCK_VM_PAGE_CLUSTER = "stock_vm_page_cluster";
    private static final String KEY_STOCK_VM_DIRTY_PROFILE = "stock_vm_dirty_profile";
    private static final String KEY_STOCK_IO_SCHEDULER = "stock_io_scheduler";
    private static final String KEY_STOCK_IO_READ_AHEAD = "stock_io_read_ahead";
    private static final String KEY_STOCK_NET_TCP = "stock_net_tcp";

    private static final String PRESET_DEFAULT = "default";
    private static final String PRESET_BATTERY = "battery";
    private static final String PRESET_GAMING = "gaming";
    private static final String PRESET_CUSTOM = "custom";

    private int mCurrentTab = TAB_GENERAL;

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

    // I/O & Network preferences
    private SwitchPreferenceCompat mIoNetEnablePref;
    private ListPreference mIoSchedulerPref;
    private ListPreference mIoReadAheadPref;
    private ListPreference mNetTcpPref;

    // Dynamic defaults loaded from sysfs/prefs
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
    private String mIoDefaultScheduler;
    private String mIoDefaultReadAhead;
    private String mNetDefaultTcp;

    public static PowertoolsSettingsFragment newInstance(int tab) {
        PowertoolsSettingsFragment fragment = new PowertoolsSettingsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB, tab);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Bundle args = getArguments();
        mCurrentTab = args != null ? args.getInt(ARG_TAB, TAB_GENERAL) : TAB_GENERAL;

        int xmlRes = switch (mCurrentTab) {
            case TAB_CPU -> R.xml.powertools_tab_cpu;
            case TAB_GPU -> R.xml.powertools_tab_gpu;
            case TAB_ZRAM -> R.xml.powertools_tab_zram;
            case TAB_IO_NET -> R.xml.powertools_tab_ionet;
            default -> R.xml.powertools_tab_general;
        };
        setPreferencesFromResource(xmlRes, rootKey);

        // Always load dynamic defaults so Presets / Reset work from TAB_GENERAL too
        loadDynamicDefaults();

        switch (mCurrentTab) {
            case TAB_GENERAL -> setupGeneralTab();
            case TAB_CPU -> setupCpuTab();
            case TAB_GPU -> setupGpuTab();
            case TAB_ZRAM -> setupZramTab();
            case TAB_IO_NET -> setupIoNetTab();
            default -> setupGeneralTab();
        }
    }

    private void loadDynamicDefaults() {
        String[] gpuFreqs = GPUUtils.getAvailableFrequencies();
        if (gpuFreqs.length > 0) {
            mGpuDefaultMin = gpuFreqs[0];
            mGpuDefaultMax = gpuFreqs[gpuFreqs.length - 1];
        }
        String[] gpuGovs = GPUUtils.getAvailableGovernors();
        String curGpuGov = GPUUtils.getCurrentGovernor();
        mGpuDefaultGov = !TextUtils.isEmpty(curGpuGov) ? curGpuGov
                : (gpuGovs.length > 0 ? gpuGovs[0] : null);

        loadCpuClusterDefaults(CPUUtils.POLICY_LITTLE,
                v -> mCpuLittleDefaultMin = v,
                v -> mCpuLittleDefaultMax = v,
                v -> mCpuLittleDefaultGov = v);
        loadCpuClusterDefaults(CPUUtils.POLICY_BIG,
                v -> mCpuBigDefaultMin = v,
                v -> mCpuBigDefaultMax = v,
                v -> mCpuBigDefaultGov = v);
        loadCpuClusterDefaults(CPUUtils.POLICY_PRIME,
                v -> mCpuPrimeDefaultMin = v,
                v -> mCpuPrimeDefaultMax = v,
                v -> mCpuPrimeDefaultGov = v);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        captureZramStockDefaults(prefs);
        captureIoNetStockDefaults(prefs);
    }

    private void loadCpuClusterDefaults(int policyCpu,
            StringConsumer minOut, StringConsumer maxOut, StringConsumer govOut) {
        String[] freqs = CPUUtils.getAvailableFrequencies(policyCpu);
        if (freqs.length > 0) {
            minOut.accept(freqs[0]);
            maxOut.accept(freqs[freqs.length - 1]);
        }
        String[] govs = CPUUtils.getAvailableGovernors(policyCpu);
        String curGov = CPUUtils.getCurrentGovernor(policyCpu);
        String defGov = !TextUtils.isEmpty(curGov) ? curGov : (govs.length > 0 ? govs[0] : null);
        if (defGov != null) {
            govOut.accept(defGov);
        }
    }

    private void captureZramStockDefaults(SharedPreferences prefs) {
        boolean zramActive = prefs.getBoolean(KEY_ZRAM_ENABLE, false);
        String[] algos = ZramUtils.getAvailableAlgorithms();
        String curSize = ZramUtils.getCurrentSizeMb();
        String curAlgo = ZramUtils.getCurrentAlgorithm();
        String curSwap = ZramUtils.getCurrentSwappiness();
        String curVfs = ZramUtils.getCurrentVfsCachePressure();
        String curCluster = ZramUtils.getCurrentPageCluster();
        String curDirty = ZramUtils.getCurrentDirtyProfile();

        if (!prefs.contains(KEY_STOCK_ZRAM_SIZE)) {
            prefs.edit()
                    .putString(KEY_STOCK_ZRAM_SIZE,
                            (!zramActive && !TextUtils.isEmpty(curSize)) ? curSize : "3584")
                    .putString(KEY_STOCK_ZRAM_ALGO,
                            (!zramActive && !TextUtils.isEmpty(curAlgo)) ? curAlgo
                                    : (algos.length > 0 ? algos[0] : "lzo-rle"))
                    .putString(KEY_STOCK_VM_SWAPPINESS,
                            (!zramActive && !TextUtils.isEmpty(curSwap)) ? curSwap : "100")
                    .putString(KEY_STOCK_VM_VFS_PRESSURE,
                            (!zramActive && !TextUtils.isEmpty(curVfs)) ? curVfs : "50")
                    .putString(KEY_STOCK_VM_PAGE_CLUSTER,
                            (!zramActive && !TextUtils.isEmpty(curCluster)) ? curCluster : "0")
                    .putString(KEY_STOCK_VM_DIRTY_PROFILE,
                            (!zramActive && !TextUtils.isEmpty(curDirty)) ? curDirty : "default")
                    .apply();
        }

        mZramDefaultSize = prefs.getString(KEY_STOCK_ZRAM_SIZE, "3584");
        mZramDefaultAlgo = prefs.getString(KEY_STOCK_ZRAM_ALGO,
                algos.length > 0 ? algos[0] : "lzo-rle");
        mVmDefaultSwappiness = prefs.getString(KEY_STOCK_VM_SWAPPINESS, "100");
        mVmDefaultVfsPressure = prefs.getString(KEY_STOCK_VM_VFS_PRESSURE, "50");
        mVmDefaultPageCluster = prefs.getString(KEY_STOCK_VM_PAGE_CLUSTER, "0");
        mVmDefaultDirtyProfile = prefs.getString(KEY_STOCK_VM_DIRTY_PROFILE, "default");
    }

    private void captureIoNetStockDefaults(SharedPreferences prefs) {
        boolean ioActive = prefs.getBoolean(KEY_IO_NET_ENABLE, false);
        String[] scheds = IoNetUtils.getAvailableSchedulers();
        String[] tcps = IoNetUtils.getAvailableTcpAlgorithms();
        String curSched = IoNetUtils.getCurrentScheduler();
        String curRa = IoNetUtils.getCurrentReadAheadKb();
        String curTcp = IoNetUtils.getCurrentTcpAlgorithm();

        if (!prefs.contains(KEY_STOCK_IO_SCHEDULER)) {
            prefs.edit()
                    .putString(KEY_STOCK_IO_SCHEDULER,
                            (!ioActive && !TextUtils.isEmpty(curSched)) ? curSched
                                    : (scheds.length > 0 ? scheds[0] : "mq-deadline"))
                    .putString(KEY_STOCK_IO_READ_AHEAD,
                            (!ioActive && !TextUtils.isEmpty(curRa)) ? curRa : "512")
                    .putString(KEY_STOCK_NET_TCP,
                            (!ioActive && !TextUtils.isEmpty(curTcp)) ? curTcp
                                    : (tcps.length > 0 ? tcps[0] : "cubic"))
                    .apply();
        }

        mIoDefaultScheduler = prefs.getString(KEY_STOCK_IO_SCHEDULER,
                scheds.length > 0 ? scheds[0] : "mq-deadline");
        mIoDefaultReadAhead = prefs.getString(KEY_STOCK_IO_READ_AHEAD, "512");
        mNetDefaultTcp = prefs.getString(KEY_STOCK_NET_TCP,
                tcps.length > 0 ? tcps[0] : "cubic");
    }

    private void setupGeneralTab() {
        mLiveCpuClocksPref = findPreference(KEY_LIVE_CPU_CLOCKS);
        mLiveGpuTempPref = findPreference(KEY_LIVE_GPU_TEMP);

        mPowerPresetPref = findPreference(KEY_POWER_PRESET);
        if (mPowerPresetPref != null) {
            mPowerPresetPref.setOnPreferenceChangeListener(this);
        }
        mResetDefaultsPref = findPreference(KEY_RESET_DEFAULTS);
        if (mResetDefaultsPref != null) {
            mResetDefaultsPref.setOnPreferenceClickListener(this);
        }
    }

    private void setupGpuTab() {
        mGpuEnablePref = findPreference(KEY_GPU_ENABLE);
        mGpuMinFreqPref = findPreference(KEY_GPU_MIN_FREQ);
        mGpuMaxFreqPref = findPreference(KEY_GPU_MAX_FREQ);
        mGpuGovernorPref = findPreference(KEY_GPU_GOVERNOR);
        mGpuAdrenoIdlerPref = findPreference(KEY_GPU_ADRENO_IDLER);
        mGpuIdleTimerPref = findPreference(KEY_GPU_IDLE_TIMER);

        if (mGpuEnablePref != null) {
            mGpuEnablePref.setOnPreferenceChangeListener(this);
        }

        String[] values = GPUUtils.getAvailableFrequencies();
        String[] entries = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            entries[i] = formatGpuFreq(Long.parseLong(values[i]));
        }
        String curMin = GPUUtils.getCurrentMinFrequency();
        String curMax = GPUUtils.getCurrentMaxFrequency();
        configureListPref(mGpuMinFreqPref, entries, values,
                !TextUtils.isEmpty(curMin) ? curMin : mGpuDefaultMin);
        configureListPref(mGpuMaxFreqPref, entries, values,
                !TextUtils.isEmpty(curMax) ? curMax : mGpuDefaultMax);

        String[] govValues = GPUUtils.getAvailableGovernors();
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

    private void setupCpuTab() {
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
                mCpuLittleDefaultMin, mCpuLittleDefaultMax, mCpuLittleDefaultGov);

        mCpuBigMinFreqPref = findPreference(KEY_CPU_BIG_MIN_FREQ);
        mCpuBigMaxFreqPref = findPreference(KEY_CPU_BIG_MAX_FREQ);
        mCpuBigGovernorPref = findPreference(KEY_CPU_BIG_GOVERNOR);
        setupCpuClusterPreferences(CPUUtils.POLICY_BIG,
                mCpuBigMinFreqPref, mCpuBigMaxFreqPref, mCpuBigGovernorPref,
                mCpuBigDefaultMin, mCpuBigDefaultMax, mCpuBigDefaultGov);

        mCpuPrimeMinFreqPref = findPreference(KEY_CPU_PRIME_MIN_FREQ);
        mCpuPrimeMaxFreqPref = findPreference(KEY_CPU_PRIME_MAX_FREQ);
        mCpuPrimeGovernorPref = findPreference(KEY_CPU_PRIME_GOVERNOR);
        setupCpuClusterPreferences(CPUUtils.POLICY_PRIME,
                mCpuPrimeMinFreqPref, mCpuPrimeMaxFreqPref, mCpuPrimeGovernorPref,
                mCpuPrimeDefaultMin, mCpuPrimeDefaultMax, mCpuPrimeDefaultGov);
    }

    private void setupZramTab() {
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

        String[] algos = ZramUtils.getAvailableAlgorithms();
        String curSize = ZramUtils.getCurrentSizeMb();
        String curAlgo = ZramUtils.getCurrentAlgorithm();
        String curSwap = ZramUtils.getCurrentSwappiness();
        String curVfs = ZramUtils.getCurrentVfsCachePressure();
        String curCluster = ZramUtils.getCurrentPageCluster();
        String curDirty = ZramUtils.getCurrentDirtyProfile();

        initStaticListPref(mZramSizePref, !TextUtils.isEmpty(curSize) ? curSize : mZramDefaultSize);
        configureListPref(mZramAlgoPref, algos, algos,
                !TextUtils.isEmpty(curAlgo) ? curAlgo : mZramDefaultAlgo);
        initStaticListPref(mVmSwappinessPref,
                !TextUtils.isEmpty(curSwap) ? curSwap : mVmDefaultSwappiness);
        initStaticListPref(mVmVfsPressurePref,
                !TextUtils.isEmpty(curVfs) ? curVfs : mVmDefaultVfsPressure);
        initStaticListPref(mVmPageClusterPref,
                !TextUtils.isEmpty(curCluster) ? curCluster : mVmDefaultPageCluster);
        initStaticListPref(mVmDirtyProfilePref,
                !TextUtils.isEmpty(curDirty) ? curDirty : mVmDefaultDirtyProfile);
    }

    private void setupIoNetTab() {
        mIoNetEnablePref = findPreference(KEY_IO_NET_ENABLE);
        mIoSchedulerPref = findPreference(KEY_IO_SCHEDULER);
        mIoReadAheadPref = findPreference(KEY_IO_READ_AHEAD);
        mNetTcpPref = findPreference(KEY_NET_TCP_CONGESTION);

        if (mIoNetEnablePref != null) {
            mIoNetEnablePref.setOnPreferenceChangeListener(this);
        }

        String[] scheds = IoNetUtils.getAvailableSchedulers();
        String curSched = IoNetUtils.getCurrentScheduler();
        configureListPref(mIoSchedulerPref, scheds, scheds,
                !TextUtils.isEmpty(curSched) ? curSched : mIoDefaultScheduler);

        String curRa = IoNetUtils.getCurrentReadAheadKb();
        initStaticListPref(mIoReadAheadPref,
                !TextUtils.isEmpty(curRa) ? curRa : mIoDefaultReadAhead);

        String[] tcps = IoNetUtils.getAvailableTcpAlgorithms();
        String curTcp = IoNetUtils.getCurrentTcpAlgorithm();
        configureListPref(mNetTcpPref, tcps, tcps,
                !TextUtils.isEmpty(curTcp) ? curTcp : mNetDefaultTcp);
    }

    private interface StringConsumer {
        void accept(String value);
    }

    private void setupCpuClusterPreferences(int policyCpu,
            ListPreference minPref, ListPreference maxPref, ListPreference govPref,
            String defMin, String defMax, String defGov) {
        String[] freqValues = CPUUtils.getAvailableFrequencies(policyCpu);
        String[] freqEntries = new String[freqValues.length];
        for (int i = 0; i < freqValues.length; i++) {
            freqEntries[i] = formatCpuFreq(Long.parseLong(freqValues[i]));
        }
        String curMin = CPUUtils.getCurrentMinFrequency(policyCpu);
        String curMax = CPUUtils.getCurrentMaxFrequency(policyCpu);
        configureListPref(minPref, freqEntries, freqValues,
                !TextUtils.isEmpty(curMin) ? curMin : defMin);
        configureListPref(maxPref, freqEntries, freqValues,
                !TextUtils.isEmpty(curMax) ? curMax : defMax);

        String[] govValues = CPUUtils.getAvailableGovernors(policyCpu);
        configureListPref(govPref, govValues, govValues, defGov);
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
        if (mCurrentTab == TAB_GENERAL) {
            mLiveHandler.removeCallbacks(mLiveUpdater);
            mLiveHandler.post(mLiveUpdater);
        }
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
        if (mPowerPresetPref != null) {
            String preset = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString(KEY_POWER_PRESET, PRESET_DEFAULT);
            if (!TextUtils.equals(mPowerPresetPref.getValue(), preset)) {
                mPowerPresetPref.setValue(preset);
            }
        }
        if (mGpuEnablePref != null) {
            setGpuPrefsEnabled(mGpuEnablePref.isChecked());
        }
        if (mCpuEnablePref != null) {
            setCpuPrefsEnabled(mCpuEnablePref.isChecked());
        }
        if (mZramEnablePref != null) {
            setZramPrefsEnabled(mZramEnablePref.isChecked());
        }
        if (mIoNetEnablePref != null) {
            setIoNetPrefsEnabled(mIoNetEnablePref.isChecked());
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
        if (mCpuInputBoostPref != null) mCpuInputBoostPref.setEnabled(enabled);
        if (mCpuWaltProfilePref != null) mCpuWaltProfilePref.setEnabled(enabled);
        if (mCpuLimitPrimePref != null) mCpuLimitPrimePref.setEnabled(enabled);
        if (mCpuUclampProfilePref != null) mCpuUclampProfilePref.setEnabled(enabled);

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

    private void setZramPrefsEnabled(boolean enabled) {
        if (mZramSizePref != null) mZramSizePref.setEnabled(enabled);
        setListPrefEnabled(mZramAlgoPref, enabled);
        if (mVmSwappinessPref != null) mVmSwappinessPref.setEnabled(enabled);
        if (mVmVfsPressurePref != null) mVmVfsPressurePref.setEnabled(enabled);
        if (mVmPageClusterPref != null) mVmPageClusterPref.setEnabled(enabled);
        if (mVmDirtyProfilePref != null) mVmDirtyProfilePref.setEnabled(enabled);
    }

    private void setIoNetPrefsEnabled(boolean enabled) {
        setListPrefEnabled(mIoSchedulerPref, enabled);
        if (mIoReadAheadPref != null) mIoReadAheadPref.setEnabled(enabled);
        setListPrefEnabled(mNetTcpPref, enabled);
    }

    private void setListPrefEnabled(ListPreference pref, boolean enabled) {
        if (pref != null) {
            pref.setEnabled(enabled && pref.getEntryValues() != null
                    && pref.getEntryValues().length > 0);
        }
    }

    private void markCustomPreset() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        if (!PRESET_CUSTOM.equals(prefs.getString(KEY_POWER_PRESET, PRESET_DEFAULT))) {
            prefs.edit().putString(KEY_POWER_PRESET, PRESET_CUSTOM).apply();
        }
        if (mPowerPresetPref != null && !PRESET_CUSTOM.equals(mPowerPresetPref.getValue())) {
            mPowerPresetPref.setValue(PRESET_CUSTOM);
        }
    }

    private void restoreGpuDefaults() {
        SharedPreferences.Editor ed =
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
        if (!TextUtils.isEmpty(mGpuDefaultMin)) {
            GPUUtils.setGPUMinFrequency(mGpuDefaultMin);
            ed.putString(KEY_GPU_MIN_FREQ, mGpuDefaultMin);
            if (mGpuMinFreqPref != null) mGpuMinFreqPref.setValue(mGpuDefaultMin);
        }
        if (!TextUtils.isEmpty(mGpuDefaultMax)) {
            GPUUtils.setGPUMaxFrequency(mGpuDefaultMax);
            ed.putString(KEY_GPU_MAX_FREQ, mGpuDefaultMax);
            if (mGpuMaxFreqPref != null) mGpuMaxFreqPref.setValue(mGpuDefaultMax);
        }
        if (!TextUtils.isEmpty(mGpuDefaultGov)) {
            GPUUtils.setGPUGovernor(mGpuDefaultGov);
            ed.putString(KEY_GPU_GOVERNOR, mGpuDefaultGov);
            if (mGpuGovernorPref != null) mGpuGovernorPref.setValue(mGpuDefaultGov);
        }
        if (GPUUtils.isAdrenoIdlerAvailable()) {
            GPUUtils.setAdrenoIdler(true);
            ed.putBoolean(KEY_GPU_ADRENO_IDLER, true);
            if (mGpuAdrenoIdlerPref != null) mGpuAdrenoIdlerPref.setChecked(true);
        }
        GPUUtils.setGPUIdleTimer("40");
        ed.putString(KEY_GPU_IDLE_TIMER, "40");
        if (mGpuIdleTimerPref != null) mGpuIdleTimerPref.setValue("40");
        ed.apply();
    }

    private void restoreCpuDefaults() {
        SharedPreferences.Editor ed =
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
        CPUUtils.setInputBoost("default");
        ed.putString(KEY_CPU_INPUT_BOOST, "default");
        if (mCpuInputBoostPref != null) mCpuInputBoostPref.setValue("default");

        CPUUtils.setWaltProfile("default");
        ed.putString(KEY_CPU_WALT_PROFILE, "default");
        if (mCpuWaltProfilePref != null) mCpuWaltProfilePref.setValue("default");

        ed.putBoolean(KEY_CPU_LIMIT_PRIME, false);
        if (mCpuLimitPrimePref != null) mCpuLimitPrimePref.setChecked(false);

        CPUUtils.setUclampProfile("default");
        ed.putString(KEY_CPU_UCLAMP_PROFILE, "default");
        if (mCpuUclampProfilePref != null) mCpuUclampProfilePref.setValue("default");

        if (!TextUtils.isEmpty(mCpuLittleDefaultMin)
                && !TextUtils.isEmpty(mCpuLittleDefaultMax)
                && !TextUtils.isEmpty(mCpuLittleDefaultGov)) {
            CPUUtils.setCPULittleFreq(
                    mCpuLittleDefaultMin, mCpuLittleDefaultMax, mCpuLittleDefaultGov);
            ed.putString(KEY_CPU_LITTLE_MIN_FREQ, mCpuLittleDefaultMin);
            ed.putString(KEY_CPU_LITTLE_MAX_FREQ, mCpuLittleDefaultMax);
            ed.putString(KEY_CPU_LITTLE_GOVERNOR, mCpuLittleDefaultGov);
            if (mCpuLittleMinFreqPref != null) mCpuLittleMinFreqPref.setValue(mCpuLittleDefaultMin);
            if (mCpuLittleMaxFreqPref != null) mCpuLittleMaxFreqPref.setValue(mCpuLittleDefaultMax);
            if (mCpuLittleGovernorPref != null) mCpuLittleGovernorPref.setValue(mCpuLittleDefaultGov);
        }
        if (!TextUtils.isEmpty(mCpuBigDefaultMin)
                && !TextUtils.isEmpty(mCpuBigDefaultMax)
                && !TextUtils.isEmpty(mCpuBigDefaultGov)) {
            CPUUtils.setCPUBigFreq(mCpuBigDefaultMin, mCpuBigDefaultMax, mCpuBigDefaultGov);
            ed.putString(KEY_CPU_BIG_MIN_FREQ, mCpuBigDefaultMin);
            ed.putString(KEY_CPU_BIG_MAX_FREQ, mCpuBigDefaultMax);
            ed.putString(KEY_CPU_BIG_GOVERNOR, mCpuBigDefaultGov);
            if (mCpuBigMinFreqPref != null) mCpuBigMinFreqPref.setValue(mCpuBigDefaultMin);
            if (mCpuBigMaxFreqPref != null) mCpuBigMaxFreqPref.setValue(mCpuBigDefaultMax);
            if (mCpuBigGovernorPref != null) mCpuBigGovernorPref.setValue(mCpuBigDefaultGov);
        }
        if (!TextUtils.isEmpty(mCpuPrimeDefaultMin)
                && !TextUtils.isEmpty(mCpuPrimeDefaultMax)
                && !TextUtils.isEmpty(mCpuPrimeDefaultGov)) {
            CPUUtils.setCPUPrimeFreq(mCpuPrimeDefaultMin, mCpuPrimeDefaultMax, mCpuPrimeDefaultGov);
            ed.putString(KEY_CPU_PRIME_MIN_FREQ, mCpuPrimeDefaultMin);
            ed.putString(KEY_CPU_PRIME_MAX_FREQ, mCpuPrimeDefaultMax);
            ed.putString(KEY_CPU_PRIME_GOVERNOR, mCpuPrimeDefaultGov);
            if (mCpuPrimeMinFreqPref != null) mCpuPrimeMinFreqPref.setValue(mCpuPrimeDefaultMin);
            if (mCpuPrimeMaxFreqPref != null) mCpuPrimeMaxFreqPref.setValue(mCpuPrimeDefaultMax);
            if (mCpuPrimeGovernorPref != null) mCpuPrimeGovernorPref.setValue(mCpuPrimeDefaultGov);
        }
        ed.apply();
    }

    private void restoreZramDefaults() {
        SharedPreferences.Editor ed =
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
        ZramUtils.applyZram(mZramDefaultSize, mZramDefaultAlgo);
        if (!TextUtils.isEmpty(mZramDefaultSize)) {
            ed.putString(KEY_ZRAM_SIZE, mZramDefaultSize);
            if (mZramSizePref != null && mZramSizePref.findIndexOfValue(mZramDefaultSize) >= 0) {
                mZramSizePref.setValue(mZramDefaultSize);
            }
        }
        if (!TextUtils.isEmpty(mZramDefaultAlgo)) {
            ed.putString(KEY_ZRAM_ALGO, mZramDefaultAlgo);
            if (mZramAlgoPref != null && mZramAlgoPref.findIndexOfValue(mZramDefaultAlgo) >= 0) {
                mZramAlgoPref.setValue(mZramDefaultAlgo);
            }
        }
        if (!TextUtils.isEmpty(mVmDefaultSwappiness)) {
            ZramUtils.setSwappiness(mVmDefaultSwappiness);
            ed.putString(KEY_VM_SWAPPINESS, mVmDefaultSwappiness);
            if (mVmSwappinessPref != null
                    && mVmSwappinessPref.findIndexOfValue(mVmDefaultSwappiness) >= 0) {
                mVmSwappinessPref.setValue(mVmDefaultSwappiness);
            }
        }
        if (!TextUtils.isEmpty(mVmDefaultVfsPressure)) {
            ZramUtils.setVfsCachePressure(mVmDefaultVfsPressure);
            ed.putString(KEY_VM_VFS_PRESSURE, mVmDefaultVfsPressure);
            if (mVmVfsPressurePref != null
                    && mVmVfsPressurePref.findIndexOfValue(mVmDefaultVfsPressure) >= 0) {
                mVmVfsPressurePref.setValue(mVmDefaultVfsPressure);
            }
        }
        if (!TextUtils.isEmpty(mVmDefaultPageCluster)) {
            ZramUtils.setPageCluster(mVmDefaultPageCluster);
            ed.putString(KEY_VM_PAGE_CLUSTER, mVmDefaultPageCluster);
            if (mVmPageClusterPref != null
                    && mVmPageClusterPref.findIndexOfValue(mVmDefaultPageCluster) >= 0) {
                mVmPageClusterPref.setValue(mVmDefaultPageCluster);
            }
        }
        if (!TextUtils.isEmpty(mVmDefaultDirtyProfile)) {
            ZramUtils.setDirtyProfile(mVmDefaultDirtyProfile);
            ed.putString(KEY_VM_DIRTY_PROFILE, mVmDefaultDirtyProfile);
            if (mVmDirtyProfilePref != null
                    && mVmDirtyProfilePref.findIndexOfValue(mVmDefaultDirtyProfile) >= 0) {
                mVmDirtyProfilePref.setValue(mVmDefaultDirtyProfile);
            }
        }
        ed.apply();
    }

    private void restoreIoNetDefaults() {
        SharedPreferences.Editor ed =
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
        if (!TextUtils.isEmpty(mIoDefaultScheduler)) {
            IoNetUtils.setIoScheduler(mIoDefaultScheduler);
            ed.putString(KEY_IO_SCHEDULER, mIoDefaultScheduler);
            if (mIoSchedulerPref != null
                    && mIoSchedulerPref.findIndexOfValue(mIoDefaultScheduler) >= 0) {
                mIoSchedulerPref.setValue(mIoDefaultScheduler);
            }
        }
        if (!TextUtils.isEmpty(mIoDefaultReadAhead)) {
            IoNetUtils.setReadAheadKb(mIoDefaultReadAhead);
            ed.putString(KEY_IO_READ_AHEAD, mIoDefaultReadAhead);
            if (mIoReadAheadPref != null
                    && mIoReadAheadPref.findIndexOfValue(mIoDefaultReadAhead) >= 0) {
                mIoReadAheadPref.setValue(mIoDefaultReadAhead);
            }
        }
        if (!TextUtils.isEmpty(mNetDefaultTcp)) {
            IoNetUtils.setTcpCongestion(mNetDefaultTcp);
            ed.putString(KEY_NET_TCP_CONGESTION, mNetDefaultTcp);
            if (mNetTcpPref != null && mNetTcpPref.findIndexOfValue(mNetDefaultTcp) >= 0) {
                mNetTcpPref.setValue(mNetDefaultTcp);
            }
        }
        ed.apply();
    }

    private void applyPreset(String preset) {
        SharedPreferences.Editor ed =
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
        switch (preset) {
            case PRESET_DEFAULT -> {
                restoreGpuDefaults();
                restoreCpuDefaults();
                restoreZramDefaults();
                restoreIoNetDefaults();
                ed.putBoolean(KEY_GPU_ENABLE, false)
                        .putBoolean(KEY_CPU_ENABLE, false)
                        .putBoolean(KEY_ZRAM_ENABLE, false)
                        .putBoolean(KEY_IO_NET_ENABLE, false)
                        .apply();
                updateUI();
            }
            case PRESET_BATTERY -> {
                ed.putBoolean(KEY_GPU_ENABLE, true).putBoolean(KEY_CPU_ENABLE, true);
                String gpuBattMax = GPUUtils.getBatteryMaxFrequency();
                if (!TextUtils.isEmpty(mGpuDefaultMin)) {
                    GPUUtils.setGPUMinFrequency(mGpuDefaultMin);
                    ed.putString(KEY_GPU_MIN_FREQ, mGpuDefaultMin);
                }
                if (!TextUtils.isEmpty(gpuBattMax)) {
                    GPUUtils.setGPUMaxFrequency(gpuBattMax);
                    ed.putString(KEY_GPU_MAX_FREQ, gpuBattMax);
                }
                if (!TextUtils.isEmpty(mGpuDefaultGov)) {
                    GPUUtils.setGPUGovernor(mGpuDefaultGov);
                    ed.putString(KEY_GPU_GOVERNOR, mGpuDefaultGov);
                }
                if (GPUUtils.isAdrenoIdlerAvailable()) {
                    GPUUtils.setAdrenoIdler(true);
                    ed.putBoolean(KEY_GPU_ADRENO_IDLER, true);
                }
                GPUUtils.setGPUIdleTimer("40");
                ed.putString(KEY_GPU_IDLE_TIMER, "40");

                CPUUtils.setInputBoost("low");
                ed.putString(KEY_CPU_INPUT_BOOST, "low");
                CPUUtils.setWaltProfile("battery");
                ed.putString(KEY_CPU_WALT_PROFILE, "battery");
                ed.putBoolean(KEY_CPU_LIMIT_PRIME, true);
                CPUUtils.setUclampProfile("battery");
                ed.putString(KEY_CPU_UCLAMP_PROFILE, "battery");

                String bigBattMax = CPUUtils.getCappedMaxFrequency(CPUUtils.POLICY_BIG, 75);
                String primeBattMax = CPUUtils.getCappedMaxFrequency(CPUUtils.POLICY_PRIME, 55);
                if (!TextUtils.isEmpty(mCpuLittleDefaultMin)
                        && !TextUtils.isEmpty(mCpuLittleDefaultMax)) {
                    CPUUtils.setCPULittleFreq(
                            mCpuLittleDefaultMin, mCpuLittleDefaultMax, mCpuLittleDefaultGov);
                    ed.putString(KEY_CPU_LITTLE_MIN_FREQ, mCpuLittleDefaultMin);
                    ed.putString(KEY_CPU_LITTLE_MAX_FREQ, mCpuLittleDefaultMax);
                }
                if (!TextUtils.isEmpty(mCpuBigDefaultMin) && !TextUtils.isEmpty(bigBattMax)) {
                    CPUUtils.setCPUBigFreq(mCpuBigDefaultMin, bigBattMax, mCpuBigDefaultGov);
                    ed.putString(KEY_CPU_BIG_MIN_FREQ, mCpuBigDefaultMin);
                    ed.putString(KEY_CPU_BIG_MAX_FREQ, bigBattMax);
                }
                if (!TextUtils.isEmpty(mCpuPrimeDefaultMin) && !TextUtils.isEmpty(primeBattMax)) {
                    CPUUtils.setCPUPrimeFreq(mCpuPrimeDefaultMin, primeBattMax, mCpuPrimeDefaultGov);
                    ed.putString(KEY_CPU_PRIME_MIN_FREQ, mCpuPrimeDefaultMin);
                    ed.putString(KEY_CPU_PRIME_MAX_FREQ, primeBattMax);
                }
                ed.apply();
                updateUI();
            }
            case PRESET_GAMING -> {
                ed.putBoolean(KEY_GPU_ENABLE, true).putBoolean(KEY_CPU_ENABLE, true);
                if (!TextUtils.isEmpty(mGpuDefaultMin)) {
                    GPUUtils.setGPUMinFrequency(mGpuDefaultMin);
                    ed.putString(KEY_GPU_MIN_FREQ, mGpuDefaultMin);
                }
                if (!TextUtils.isEmpty(mGpuDefaultMax)) {
                    GPUUtils.setGPUMaxFrequency(mGpuDefaultMax);
                    ed.putString(KEY_GPU_MAX_FREQ, mGpuDefaultMax);
                }
                if (!TextUtils.isEmpty(mGpuDefaultGov)) {
                    GPUUtils.setGPUGovernor(mGpuDefaultGov);
                    ed.putString(KEY_GPU_GOVERNOR, mGpuDefaultGov);
                }
                if (GPUUtils.isAdrenoIdlerAvailable()) {
                    GPUUtils.setAdrenoIdler(false);
                    ed.putBoolean(KEY_GPU_ADRENO_IDLER, false);
                }
                GPUUtils.setGPUIdleTimer("80");
                ed.putString(KEY_GPU_IDLE_TIMER, "80");

                CPUUtils.setInputBoost("high");
                ed.putString(KEY_CPU_INPUT_BOOST, "high");
                CPUUtils.setWaltProfile("responsive");
                ed.putString(KEY_CPU_WALT_PROFILE, "responsive");
                ed.putBoolean(KEY_CPU_LIMIT_PRIME, false);
                CPUUtils.setUclampProfile("performance");
                ed.putString(KEY_CPU_UCLAMP_PROFILE, "performance");

                if (!TextUtils.isEmpty(mCpuLittleDefaultMin)
                        && !TextUtils.isEmpty(mCpuLittleDefaultMax)) {
                    CPUUtils.setCPULittleFreq(
                            mCpuLittleDefaultMin, mCpuLittleDefaultMax, mCpuLittleDefaultGov);
                    ed.putString(KEY_CPU_LITTLE_MIN_FREQ, mCpuLittleDefaultMin);
                    ed.putString(KEY_CPU_LITTLE_MAX_FREQ, mCpuLittleDefaultMax);
                }
                if (!TextUtils.isEmpty(mCpuBigDefaultMin)
                        && !TextUtils.isEmpty(mCpuBigDefaultMax)) {
                    CPUUtils.setCPUBigFreq(
                            mCpuBigDefaultMin, mCpuBigDefaultMax, mCpuBigDefaultGov);
                    ed.putString(KEY_CPU_BIG_MIN_FREQ, mCpuBigDefaultMin);
                    ed.putString(KEY_CPU_BIG_MAX_FREQ, mCpuBigDefaultMax);
                }
                if (!TextUtils.isEmpty(mCpuPrimeDefaultMin)
                        && !TextUtils.isEmpty(mCpuPrimeDefaultMax)) {
                    CPUUtils.setCPUPrimeFreq(
                            mCpuPrimeDefaultMin, mCpuPrimeDefaultMax, mCpuPrimeDefaultGov);
                    ed.putString(KEY_CPU_PRIME_MIN_FREQ, mCpuPrimeDefaultMin);
                    ed.putString(KEY_CPU_PRIME_MAX_FREQ, mCpuPrimeDefaultMax);
                }
                ed.apply();
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
            restoreIoNetDefaults();
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                    .putBoolean(KEY_GPU_ENABLE, false)
                    .putBoolean(KEY_CPU_ENABLE, false)
                    .putBoolean(KEY_ZRAM_ENABLE, false)
                    .putBoolean(KEY_IO_NET_ENABLE, false)
                    .putString(KEY_POWER_PRESET, PRESET_DEFAULT)
                    .apply();
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

        // GPU
        if (preference == mGpuEnablePref) {
            boolean enable = (Boolean) newValue;
            if (!enable) restoreGpuDefaults();
            setGpuPrefsEnabled(enable);
            markCustomPreset();
            return true;
        }
        if (preference == mGpuMinFreqPref) {
            String newMin = newValue.toString();
            if (Long.parseLong(newMin) > Long.parseLong(mGpuMaxFreqPref.getValue())) {
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
            if (Long.parseLong(newMax) < Long.parseLong(mGpuMinFreqPref.getValue())) {
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

        // CPU
        if (preference == mCpuEnablePref) {
            boolean enable = (Boolean) newValue;
            if (!enable) restoreCpuDefaults();
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
                    if (!TextUtils.isEmpty(minFreq)) mCpuPrimeMinFreqPref.setValue(minFreq);
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

        // ZRAM & VM
        if (preference == mZramEnablePref) {
            boolean enable = (Boolean) newValue;
            if (!enable) restoreZramDefaults();
            setZramPrefsEnabled(enable);
            markCustomPreset();
            return true;
        }
        if (preference == mZramSizePref) {
            String algo = mZramAlgoPref != null ? mZramAlgoPref.getValue() : mZramDefaultAlgo;
            ZramUtils.applyZram(newValue.toString(), algo);
            markCustomPreset();
            return true;
        }
        if (preference == mZramAlgoPref) {
            String size = mZramSizePref != null ? mZramSizePref.getValue() : mZramDefaultSize;
            ZramUtils.applyZram(size, newValue.toString());
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

        // I/O & Network
        if (preference == mIoNetEnablePref) {
            boolean enable = (Boolean) newValue;
            if (!enable) restoreIoNetDefaults();
            setIoNetPrefsEnabled(enable);
            markCustomPreset();
            return true;
        }
        if (preference == mIoSchedulerPref) {
            IoNetUtils.setIoScheduler(newValue.toString());
            markCustomPreset();
            return true;
        }
        if (preference == mIoReadAheadPref) {
            IoNetUtils.setReadAheadKb(newValue.toString());
            markCustomPreset();
            return true;
        }
        if (preference == mNetTcpPref) {
            IoNetUtils.setTcpCongestion(newValue.toString());
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
