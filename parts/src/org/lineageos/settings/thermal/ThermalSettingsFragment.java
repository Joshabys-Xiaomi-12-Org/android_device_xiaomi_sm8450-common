/*
 * Copyright (C) 2020 The LineageOS Project
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

package org.lineageos.settings.thermal;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThermalSettingsFragment extends SettingsBasePreferenceFragment {

    private ThermalUtils mThermalUtils;
    private Preference mGlobalProfilePref;
    private PreferenceCategory mAppsCategory;

    private final List<AppEntry> mAllEntries = new ArrayList<>();
    private String mSearchQuery = "";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private static final class AppEntry {
        final String packageName;
        final String label;
        final Drawable icon;
        int state;

        AppEntry(String packageName, String label, Drawable icon, int state) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.state = state;
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = requireContext();
        mThermalUtils = new ThermalUtils(context);

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        PreferenceCategory globalCategory = new PreferenceCategory(context);
        globalCategory.setTitle(R.string.thermal_global_category);
        screen.addPreference(globalCategory);

        mGlobalProfilePref = new Preference(context);
        mGlobalProfilePref.setKey("thermal_global_profile_entry");
        mGlobalProfilePref.setTitle(R.string.thermal_global_title);
        mGlobalProfilePref.setSummary(
                ThermalUtils.getProfileNameRes(mThermalUtils.getGlobalProfile()));
        mGlobalProfilePref.setPersistent(false);
        mGlobalProfilePref.setOnPreferenceClickListener(p -> {
            startActivity(new Intent(requireContext(), ThermalGlobalProfileActivity.class));
            return true;
        });
        globalCategory.addPreference(mGlobalProfilePref);

        mAppsCategory = new PreferenceCategory(context);
        mAppsCategory.setTitle(R.string.thermal_apps_title);
        screen.addPreference(mAppsCategory);
        setPreferenceScreen(screen);

        loadAppsAsync();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGlobalProfilePref != null && mThermalUtils != null) {
            mGlobalProfilePref.setSummary(
                    ThermalUtils.getProfileNameRes(mThermalUtils.getGlobalProfile()));
        }
        if (!mAllEntries.isEmpty()) {
            refreshEntryStates();
            renderFilteredApps();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }

    public void setSearchQuery(String query) {
        String normalized = query != null ? query.trim() : "";
        if (!normalized.equals(mSearchQuery)) {
            mSearchQuery = normalized;
            renderFilteredApps();
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
                int state = mThermalUtils.getStateForPackage(pkg);
                entries.add(new AppEntry(pkg, label, icon, state));
            }

            sortEntries(entries);

            mMainHandler.post(() -> {
                if (!isAdded() || getContext() == null) {
                    return;
                }
                mAllEntries.clear();
                mAllEntries.addAll(entries);
                renderFilteredApps();
            });
        });
    }

    private void refreshEntryStates() {
        for (AppEntry entry : mAllEntries) {
            entry.state = mThermalUtils.getStateForPackage(entry.packageName);
        }
        sortEntries(mAllEntries);
    }

    private static void sortEntries(List<AppEntry> entries) {
        Collator collator = Collator.getInstance();
        entries.sort(Comparator.comparing((AppEntry e) -> e.state == ThermalUtils.STATE_DEFAULT)
                .thenComparing(e -> e.label, collator));
    }

    private void renderFilteredApps() {
        if (!isAdded() || getContext() == null || mAppsCategory == null) {
            return;
        }
        mAppsCategory.removeAll();
        Context prefContext = getPreferenceManager().getContext();
        String lowerQuery = mSearchQuery.toLowerCase(Locale.getDefault());

        for (AppEntry entry : mAllEntries) {
            if (!TextUtils.isEmpty(lowerQuery)
                    && !entry.label.toLowerCase(Locale.getDefault()).contains(lowerQuery)
                    && !entry.packageName.toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                continue;
            }
            Preference pref = new Preference(prefContext);
            pref.setKey(entry.packageName);
            pref.setTitle(entry.label);
            pref.setIcon(entry.icon);
            pref.setSummary(ThermalUtils.getProfileNameRes(entry.state));
            pref.setPersistent(false);
            pref.setOnPreferenceClickListener(p -> {
                Intent intent = new Intent(requireContext(), ThermalProfileActivity.class);
                intent.putExtra(ThermalProfileFragment.EXTRA_PACKAGE_NAME, entry.packageName);
                intent.putExtra(ThermalProfileFragment.EXTRA_APP_LABEL, entry.label);
                startActivity(intent);
                return true;
            });
            mAppsCategory.addPreference(pref);
        }
    }
}
