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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RefreshFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private SwitchPreferenceCompat mEnablePref;
    private PreferenceCategory mAppsCategory;
    private RefreshUtils mRefreshUtils;

    private String[] mRateEntries;
    private String[] mRateValues;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private static final class AppEntry {
        final String packageName;
        final String label;
        final Drawable icon;
        final int savedRate;

        AppEntry(String packageName, String label, Drawable icon, int savedRate) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.savedRate = savedRate;
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.refresh_rate_settings, rootKey);

        Context context = requireContext();
        mRefreshUtils = new RefreshUtils(context);
        buildRateOptions(context);

        mEnablePref = findPreference(RefreshUtils.KEY_ENABLED);
        mAppsCategory = findPreference("refresh_rate_apps_category");

        if (mEnablePref != null) {
            mEnablePref.setChecked(mRefreshUtils.isEnabled());
            mEnablePref.setOnPreferenceChangeListener(this);
        }

        loadAppsAsync();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }

    private void buildRateOptions(Context context) {
        int[] rates = RefreshUtils.getSupportedRefreshRates(context);
        mRateEntries = new String[rates.length + 1];
        mRateValues = new String[rates.length + 1];
        mRateEntries[0] = getString(R.string.refresh_rate_default);
        mRateValues[0] = String.valueOf(RefreshUtils.RATE_DEFAULT);
        for (int i = 0; i < rates.length; i++) {
            mRateEntries[i + 1] = rates[i] + " Hz";
            mRateValues[i + 1] = String.valueOf(rates[i]);
        }
    }

    private void loadAppsAsync() {
        Context appContext = requireContext().getApplicationContext();
        mExecutor.execute(() -> {
            PackageManager pm = appContext.getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolved = pm.queryIntentActivities(mainIntent, 0);

            Set<String> seen = new HashSet<>();
            List<AppEntry> entries = new ArrayList<>();
            for (ResolveInfo info : resolved) {
                if (info.activityInfo == null) {
                    continue;
                }
                String pkg = info.activityInfo.packageName;
                if (pkg == null || !seen.add(pkg)) {
                    continue;
                }
                String label = info.loadLabel(pm).toString();
                Drawable icon = info.loadIcon(pm);
                int savedRate = mRefreshUtils.getRateForPackage(pkg);
                entries.add(new AppEntry(pkg, label, icon, savedRate));
            }

            Collator collator = Collator.getInstance();
            entries.sort(Comparator.comparing((AppEntry e) -> e.savedRate == 0)
                    .thenComparing(e -> e.label, collator));

            mMainHandler.post(() -> populateAppPreferences(entries));
        });
    }

    private void populateAppPreferences(List<AppEntry> entries) {
        if (!isAdded() || getContext() == null || mAppsCategory == null) {
            return;
        }
        mAppsCategory.removeAll();
        boolean enabled = mRefreshUtils.isEnabled();
        Context prefContext = getPreferenceManager().getContext();

        for (AppEntry entry : entries) {
            ListPreference pref = new ListPreference(prefContext);
            pref.setKey(entry.packageName);
            pref.setTitle(entry.label);
            pref.setDialogTitle(entry.label);
            pref.setIcon(entry.icon);
            pref.setEntries(mRateEntries);
            pref.setEntryValues(mRateValues);
            pref.setPersistent(false);
            pref.setSummary("%s");
            String val = String.valueOf(entry.savedRate);
            pref.setValue(pref.findIndexOfValue(val) >= 0 ? val : mRateValues[0]);
            pref.setEnabled(enabled);
            pref.setOnPreferenceChangeListener(this);
            mAppsCategory.addPreference(pref);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEnablePref) {
            boolean enabled = (Boolean) newValue;
            mRefreshUtils.setEnabled(enabled);
            if (enabled) {
                RefreshUtils.startService(requireContext());
            }
            if (mAppsCategory != null) {
                for (int i = 0; i < mAppsCategory.getPreferenceCount(); i++) {
                    mAppsCategory.getPreference(i).setEnabled(enabled);
                }
            }
            return true;
        }

        if (preference instanceof ListPreference) {
            String pkg = preference.getKey();
            int rate = Integer.parseInt(newValue.toString());
            mRefreshUtils.setRateForPackage(pkg, rate);
            return true;
        }

        return true;
    }
}
