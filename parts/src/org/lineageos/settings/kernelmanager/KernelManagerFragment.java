/*
 * Copyright (C) 2025 KamiKaonashi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package org.lineageos.settings.kernelmanager;

import android.os.Bundle;
import android.widget.Toast;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import org.lineageos.settings.R;

public class KernelManagerFragment extends PreferenceFragment 
    implements Preference.OnPreferenceChangeListener {

    private static final String KEY_CPU_GOVERNOR = "cpu_governor";
    private static final String KEY_LITTLE_MIN_FREQ = "little_min_freq";
    private static final String KEY_LITTLE_MAX_FREQ = "little__max_freq";
    private static final String KEY_BIG_MIN_FREQ = "big_min_freq";
    private static final String KEY_BIG_MAX_FREQ = "big_max_freq";
    private static final String KEY_PRIME_MIN_FREQ = "prime_min_freq";
    private static final String KEY_PRIME_MAX_FREQ = "prime_max_freq";
    private static final String KEY_APPLY_SETTINGS = "apply_settings";
    private static final String KEY_RESET_SETTINGS = "reset_settings";
    
    private KernelManagerUtils mKernelUtils;
    private ListPreference mGovernorPreference;
    private ListPreference mLittleMinFreq, mLittleMaxFreq;
    private ListPreference mBigMinFreq, mBigMaxFreq;
    private ListPreference mPrimeMinFreq, mPrimeMaxFreq;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.kernel_manager_settings, rootKey);
        mKernelUtils = new KernelManagerUtils();
        
        initializePreferences();
        loadCurrentSettings();
    }

    private void initializePreferences() {
        mGovernorPreference = (ListPreference) findPreference(KEY_CPU_GOVERNOR);
        mLittleMinFreq = (ListPreference) findPreference(KEY_EFFICIENCY_MIN_FREQ);
        mLittleMaxFreq = (ListPreference) findPreference(KEY_EFFICIENCY_MAX_FREQ);
        mBigMinFreq = (ListPreference) findPreference(KEY_PERFORMANCE_MIN_FREQ);
        mBigMaxFreq = (ListPreference) findPreference(KEY_PERFORMANCE_MAX_FREQ);
        mPrimeMinFreq = (ListPreference) findPreference(KEY_PRIME_MIN_FREQ);
        mPrimeMaxFreq = (ListPreference) findPreference(KEY_PRIME_MAX_FREQ);
        
        // Set listeners
        if (mGovernorPreference != null) {
            mGovernorPreference.setOnPreferenceChangeListener(this);
        }
        
        setFrequencyPreferenceListeners();
        
        // Apply and Reset buttons
        Preference applyPref = findPreference(KEY_APPLY_SETTINGS);
        if (applyPref != null) {
            applyPref.setOnPreferenceClickListener(preference -> {
                applySettings();
                return true;
            });
        }
        
        Preference resetPref = findPreference(KEY_RESET_SETTINGS);
        if (resetPref != null) {
            resetPref.setOnPreferenceClickListener(preference -> {
                resetSettings();
                return true;
            });
        }
    }

    private void setFrequencyPreferenceListeners() {
        if (mLittleMinFreq != null) mLittleMinFreq.setOnPreferenceChangeListener(this);
        if (mLittleMaxFreq != null) mLittleMaxFreq.setOnPreferenceChangeListener(this);
        if (mBigMinFreq != null) mBigMinFreq.setOnPreferenceChangeListener(this);
        if (mBigMaxFreq != null) mBigMaxFreq.setOnPreferenceChangeListener(this);
        if (mPrimeMinFreq != null) mPrimeMinFreq.setOnPreferenceChangeListener(this);
        if (mPrimeMaxFreq != null) mPrimeMaxFreq.setOnPreferenceChangeListener(this);
    }

    private void loadCurrentSettings() {
        // Load available governors
        String[] governors = mKernelUtils.getAvailableGovernors();
        if (governors != null && mGovernorPreference != null) {
            mGovernorPreference.setEntries(governors);
            mGovernorPreference.setEntryValues(governors);
            String currentGovernor = mKernelUtils.getCurrentGovernor(KernelManagerUtils.LITTLE_CLUSTER);
            mGovernorPreference.setValue(currentGovernor);
            mGovernorPreference.setSummary(getString(R.string.cpu_governor_summary, currentGovernor));
        }
        
        // Load available frequencies for each cluster
        loadFrequenciesForCluster(KernelManagerUtils.LITTLE_CLUSTER, mLittleMinFreq, mLittleMaxFreq);
        loadFrequenciesForCluster(KernelManagerUtils.BIG_CLUSTER, mBigMinFreq, mBigMaxFreq);
        loadFrequenciesForCluster(KernelManagerUtils.PRIME_CLUSTER, mPrimeMinFreq, mPrimeMaxFreq);
    }

    private void loadFrequenciesForCluster(int cluster, ListPreference minPref, ListPreference maxPref) {
        String[] frequencies = mKernelUtils.getAvailableFrequencies(cluster);
        if (frequencies != null) {
            String[] frequencyLabels = new String[frequencies.length];
            for (int i = 0; i < frequencies.length; i++) {
                int freqMhz = Integer.parseInt(frequencies[i]) / 1000;
                frequencyLabels[i] = freqMhz + " MHz";
            }
            
            if (minPref != null) {
                minPref.setEntries(frequencyLabels);
                minPref.setEntryValues(frequencies);
                String currentMinFreq = mKernelUtils.getCurrentMinFrequency(cluster);
                minPref.setValue(currentMinFreq);
                int minFreqMhz = Integer.parseInt(currentMinFreq) / 1000;
                minPref.setSummary(minFreqMhz + " MHz");
            }
            
            if (maxPref != null) {
                maxPref.setEntries(frequencyLabels);
                maxPref.setEntryValues(frequencies);
                String currentMaxFreq = mKernelUtils.getCurrentMaxFrequency(cluster);
                maxPref.setValue(currentMaxFreq);
                int maxFreqMhz = Integer.parseInt(currentMaxFreq) / 1000;
                maxPref.setSummary(maxFreqMhz + " MHz");
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        String value = (String) newValue;
        
        if (KEY_CPU_GOVERNOR.equals(key)) {
            mGovernorPreference.setSummary(getString(R.string.cpu_governor_summary, value));
            return true;
        } else if (key.contains("freq")) {
            int freqMhz = Integer.parseInt(value) / 1000;
            preference.setSummary(freqMhz + " MHz");
            return true;
        }
        
        return false;
    }

    private void applySettings() {
        // Apply governor
        if (mGovernorPreference != null) {
            String governor = mGovernorPreference.getValue();
            mKernelUtils.setGovernor(governor);
        }
        
        // Apply frequencies
        applyFrequencySettings();
        
        Toast.makeText(getContext(), R.string.settings_applied, Toast.LENGTH_SHORT).show();
    }

    private void applyFrequencySettings() {
        if (mLittleMinFreq != null && mLittleMaxFreq != null) {
            mKernelUtils.setLittleClusterFrequency(
                mLittleMinFreq.getValue(), mLittleMaxFreq.getValue());
        }
        if (mBigMinFreq != null && mBigMaxFreq != null) {
            mKernelUtils.setBigClusterFrequency(
                mBigMinFreq.getValue(), mBigMaxFreq.getValue());
        }
        if (mPrimeMinFreq != null && mPrimeMaxFreq != null) {
            mKernelUtils.setPrimeClusterFrequency(
                mBigMinFreq.getValue(), mBigMaxFreq.getValue());
        }
    }

    private void resetSettings() {
        mKernelUtils.resetToDefaults();
        loadCurrentSettings();
        Toast.makeText(getContext(), R.string.settings_reset, Toast.LENGTH_SHORT).show();
    }
}
