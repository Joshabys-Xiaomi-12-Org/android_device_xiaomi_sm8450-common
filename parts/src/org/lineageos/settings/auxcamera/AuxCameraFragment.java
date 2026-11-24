/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.auxcamera;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuxCameraFragment extends SettingsBasePreferenceFragment {

    private static final String KEY_MASTER_SWITCH = "aux_camera_enable";
    private static final String KEY_APPS_CATEGORY = "aux_camera_apps";

    private MainSwitchPreference mMasterSwitch;
    private PreferenceCategory mAppsCategory;

    private final List<AppEntry> mAllApps = new ArrayList<>();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private String mQuery = "";

    private static final class AppEntry {
        final String packageName;
        final String label;
        final Drawable icon;

        AppEntry(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final Context context = getPreferenceManager().getContext();
        final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        setPreferenceScreen(screen);

        mMasterSwitch = new MainSwitchPreference(context);
        mMasterSwitch.setKey(KEY_MASTER_SWITCH);
        mMasterSwitch.setTitle(R.string.aux_camera_enable);
        mMasterSwitch.setChecked(AuxCameraUtils.isEnabled(context));
        mMasterSwitch.setOnPreferenceChangeListener((pref, newValue) -> {
            boolean enabled = (Boolean) newValue;
            AuxCameraUtils.setEnabled(context, enabled);
            updateAppPreferencesEnabled(enabled);
            return true;
        });
        screen.addPreference(mMasterSwitch);

        mAppsCategory = new PreferenceCategory(context);
        mAppsCategory.setKey(KEY_APPS_CATEGORY);
        mAppsCategory.setTitle(R.string.thermal_apps_title);
        screen.addPreference(mAppsCategory);

        loadApps(context);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }

    public void setSearchQuery(String query) {
        mQuery = query != null ? query.trim().toLowerCase(Locale.getDefault()) : "";
        renderApps();
    }

    private void loadApps(Context context) {
        final Context appContext = context.getApplicationContext();
        mExecutor.execute(() -> {
            PackageManager pm = appContext.getPackageManager();
            Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
            launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(launcherIntent, 0);
            Set<String> seen = new HashSet<>();
            List<AppEntry> loaded = new ArrayList<>();

            for (ResolveInfo ri : resolveInfos) {
                if (ri.activityInfo == null || ri.activityInfo.applicationInfo == null) {
                    continue;
                }
                ApplicationInfo ai = ri.activityInfo.applicationInfo;
                String pkg = ai.packageName;
                if (pkg == null || !seen.add(pkg)) {
                    continue;
                }
                CharSequence labelCs = ai.loadLabel(pm);
                String label = TextUtils.isEmpty(labelCs) ? pkg : labelCs.toString();
                Drawable icon = ai.loadIcon(pm);
                loaded.add(new AppEntry(pkg, label, icon));
            }

            Collator collator = Collator.getInstance();
            Collections.sort(loaded, (a, b) -> collator.compare(a.label, b.label));

            mMainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                mAllApps.clear();
                mAllApps.addAll(loaded);
                renderApps();
            });
        });
    }

    private void renderApps() {
        if (!isAdded() || mAppsCategory == null) {
            return;
        }
        Context context = getPreferenceManager().getContext();
        boolean masterEnabled = AuxCameraUtils.isEnabled(context);
        Set<String> selectedApps = AuxCameraUtils.getApps(context);

        mAppsCategory.removeAll();
        for (AppEntry entry : mAllApps) {
            if (!mQuery.isEmpty()
                    && !entry.label.toLowerCase(Locale.getDefault()).contains(mQuery)
                    && !entry.packageName.toLowerCase(Locale.getDefault()).contains(mQuery)) {
                continue;
            }
            SwitchPreferenceCompat pref = new SwitchPreferenceCompat(context);
            pref.setKey("aux_app_" + entry.packageName);
            pref.setTitle(entry.label);
            pref.setSummary(entry.packageName);
            pref.setIcon(entry.icon);
            pref.setChecked(selectedApps.contains(entry.packageName));
            pref.setEnabled(masterEnabled);
            pref.setPersistent(false);
            pref.setOnPreferenceChangeListener((p, newVal) -> {
                boolean checked = (Boolean) newVal;
                boolean ok = AuxCameraUtils.setAppEnabled(context, entry.packageName, checked);
                if (!ok) {
                    Toast.makeText(context, R.string.aux_camera_limit_reached,
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            });
            mAppsCategory.addPreference(pref);
        }
    }

    private void updateAppPreferencesEnabled(boolean enabled) {
        if (mAppsCategory == null) {
            return;
        }
        for (int i = 0; i < mAppsCategory.getPreferenceCount(); i++) {
            mAppsCategory.getPreference(i).setEnabled(enabled);
        }
    }
}
