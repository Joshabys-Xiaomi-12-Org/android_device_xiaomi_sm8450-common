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
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.ExpressiveDesignEnabledProvider;

import org.lineageos.settings.R;

public class ThermalFileEditorActivity extends CollapsingToolbarBaseActivity
        implements ExpressiveDesignEnabledProvider {

    private static final String TAG = "ThermalFileEditor";
    public static final String EXTRA_FILE_NAME = "thermal_file_name";

    private String mFileName;
    private boolean mWasEncrypted = true;

    private TextView mStatusView;
    private EditText mEditorText;
    private Button mResetButton;

    @Override
    public boolean isExpressiveDesignEnabled() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thermal_file_editor);

        mFileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        if (!MiThermalCrypt.isValidFileName(mFileName)) {
            finish();
            return;
        }
        setTitle(mFileName);

        mStatusView = findViewById(R.id.thermal_editor_status);
        mEditorText = findViewById(R.id.thermal_editor_text);
        Button helpButton = findViewById(R.id.thermal_editor_btn_help);
        mResetButton = findViewById(R.id.thermal_editor_btn_reset);
        Button saveButton = findViewById(R.id.thermal_editor_btn_save);

        helpButton.setOnClickListener(
                v -> startActivity(new Intent(this, ThermalHelpActivity.class)));

        mResetButton.setOnClickListener(v -> confirmReset());
        saveButton.setOnClickListener(v -> saveAndApply());

        loadFile();
    }

    private void loadFile() {
        try {
            MiThermalCrypt.DecryptedConfig cfg = MiThermalCrypt.loadConfig(this, mFileName);
            mWasEncrypted = cfg.wasEncrypted;
            mEditorText.setText(cfg.content);
            updateStatusBanner(cfg.isModified);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load thermal config: " + mFileName, e);
            Toast.makeText(this, R.string.thermal_editor_load_error, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void updateStatusBanner(boolean modified) {
        String source = getString(modified
                ? R.string.thermal_editor_status_modified
                : R.string.thermal_editor_status_stock);
        String format = getString(mWasEncrypted
                ? R.string.thermal_editor_format_encrypted
                : R.string.thermal_editor_format_plain);
        mStatusView.setText(source + "  •  " + format);
        mResetButton.setEnabled(modified);
    }

    private void saveAndApply() {
        String content = mEditorText.getText().toString();
        if (TextUtils.isEmpty(content.trim())) {
            Toast.makeText(this, R.string.thermal_editor_empty_error, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            MiThermalCrypt.saveAndApply(this, mFileName, content, mWasEncrypted);
            updateStatusBanner(true);
            Toast.makeText(this, R.string.thermal_editor_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save thermal config: " + mFileName, e);
            Toast.makeText(this, R.string.thermal_editor_save_error, Toast.LENGTH_LONG).show();
        }
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.thermal_editor_btn_reset)
                .setMessage(R.string.thermal_editor_reset_confirm)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    MiThermalCrypt.resetConfig(this, mFileName);
                    loadFile();
                    Toast.makeText(this, R.string.thermal_editor_reset_done,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
