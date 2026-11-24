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

import java.util.List;

public class ThermalEditorFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private Preference mHelpPref;
    private Preference mResetAllPref;
    private PreferenceCategory mFilesCategory;

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
        Context context = requireContext();
        Context prefContext = getPreferenceManager().getContext();
        List<String> files = MiThermalCrypt.listVendorThermalConfigs();

        if (files.isEmpty()) {
            Preference empty = new Preference(prefContext);
            empty.setTitle(R.string.thermal_editor_no_files);
            empty.setSelectable(false);
            mFilesCategory.addPreference(empty);
            return;
        }

        for (String name : files) {
            Preference pref = new Preference(prefContext);
            pref.setKey(name);
            pref.setTitle(name);
            boolean modified = MiThermalCrypt.isModified(context, name);
            pref.setSummary(modified
                    ? R.string.thermal_editor_status_modified
                    : R.string.thermal_editor_status_stock);
            pref.setOnPreferenceClickListener(this);
            mFilesCategory.addPreference(pref);
        }
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
            startActivity(intent);
            return true;
        }

        return false;
    }
}
