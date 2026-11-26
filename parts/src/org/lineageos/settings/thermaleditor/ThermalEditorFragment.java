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

package org.lineageos.settings.thermaleditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;
import org.lineageos.settings.thermal.ThermalMapResolver;
import org.lineageos.settings.thermal.ThermalUtils;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ThermalEditorFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private static final String VENDOR_ETC = "/vendor/etc";

    private Preference mHelpPref;
    private Preference mResetAllPref;
    private PreferenceCategory mFilesCategory;

    private final Map<String, String> mFileTitles = new HashMap<>();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.thermal_editor_settings, rootKey);

        mHelpPref = findPreference("thermal_editor_help");
        if (mHelpPref != null) {
            mHelpPref.setOnPreferenceClickListener(this);
        }

        mResetAllPref = findPreference("thermal_editor_reset_all");
        if (mResetAllPref != null) {
            mResetAllPref.setOnPreferenceClickListener(this);
        }

        mFilesCategory = findPreference("thermal_editor_files_category");
    }

    @Override
    public void onResume() {
        super.onResume();
        populateThermalFiles();
    }

    private void populateThermalFiles() {
        if (mFilesCategory == null || getContext() == null) {
            return;
        }
        mFilesCategory.removeAll();
        mFileTitles.clear();

        Context context = requireContext();
        Context prefContext = getPreferenceManager().getContext();
        ThermalMapResolver resolver = ThermalMapResolver.getInstance();
        boolean thermalDisabled = ThermalUtils.isThermalDisabled();

        if (mResetAllPref != null) {
            mResetAllPref.setEnabled(!thermalDisabled);
        }

        Set<String> seenFiles = new HashSet<>();
        int added = 0;

        // Primary: list clearly identified, meaningful profiles from thermal-map.conf
        for (int state = 0; state < ThermalUtils.PROFILE_COUNT; state++) {
            if (!ThermalUtils.isProfileAvailable(state)) {
                continue;
            }
            String fileName = resolver.getConfigFileForState(state);
            if (!MiThermalCrypt.isValidFileName(fileName) || !seenFiles.add(fileName)) {
                continue;
            }
            if (!new File(VENDOR_ETC, fileName).isFile()) {
                continue;
            }
            String friendlyName = getString(ThermalUtils.getProfileNameRes(state));
            addFilePreference(context, prefContext, fileName, friendlyName);
            added++;
        }

        // Fallback if thermal-map.conf was unavailable on a device: show filtered vendor files
        if (added == 0) {
            List<String> files = MiThermalCrypt.listVendorThermalConfigs();
            for (String fileName : files) {
                if (!isUsefulFallbackFile(fileName) || !seenFiles.add(fileName)) {
                    continue;
                }
                addFilePreference(context, prefContext, fileName, fileName);
                added++;
            }
        }

        if (added == 0) {
            Preference empty = new Preference(prefContext);
            empty.setTitle(R.string.thermal_editor_no_files);
            empty.setSelectable(false);
            mFilesCategory.addPreference(empty);
        }
    }

    private void addFilePreference(Context context, Context prefContext,
            String fileName, String displayTitle) {
        mFileTitles.put(fileName, displayTitle);
        boolean modified = MiThermalCrypt.isModified(context, fileName);
        String status = getString(modified
                ? R.string.thermal_editor_status_modified
                : R.string.thermal_editor_status_stock);

        Preference pref = new Preference(prefContext);
        pref.setKey(fileName);
        pref.setTitle(displayTitle);
        pref.setSummary(fileName + "  •  " + status);
        pref.setEnabled(!ThermalUtils.isThermalDisabled());
        pref.setOnPreferenceClickListener(this);
        mFilesCategory.addPreference(pref);
    }

    private static boolean isUsefulFallbackFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return !lower.contains("map")
                && !lower.contains("iec")
                && !lower.contains("unfold")
                && !lower.contains("abnormal")
                && !lower.contains("chg-only")
                && !lower.contains("-per-")
                && !lower.contains("-hp-")
                && !lower.contains("devices")
                && !lower.contains("engine")
                && !lower.contains("sptm")
                && !lower.contains("cclassvideo")
                && !lower.contains("nightvideo")
                && !lower.contains("dolbyvision");
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mHelpPref) {
            startActivity(new Intent(requireContext(), ThermalHelpActivity.class));
            return true;
        }

        if (preference == mResetAllPref) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.thermal_editor_reset_all_title)
                    .setMessage(R.string.thermal_editor_reset_all_confirm)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        MiThermalCrypt.resetAllConfigs(requireContext());
                        populateThermalFiles();
                        Toast.makeText(getActivity(),
                                R.string.thermal_editor_reset_all_done,
                                Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        }

        String fileName = preference.getKey();
        if (MiThermalCrypt.isValidFileName(fileName)) {
            Intent intent = new Intent(requireContext(), ThermalFileEditorActivity.class);
            intent.putExtra(ThermalFileEditorActivity.EXTRA_FILE_NAME, fileName);
            String title = mFileTitles.get(fileName);
            if (title != null) {
                intent.putExtra(ThermalFileEditorActivity.EXTRA_PROFILE_TITLE, title);
            }
            startActivity(intent);
            return true;
        }

        return false;
    }
}
