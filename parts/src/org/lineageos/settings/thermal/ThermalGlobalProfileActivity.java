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

package org.lineageos.settings.thermal;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.service.quicksettings.TileService;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.ExpressiveDesignEnabledProvider;

import org.lineageos.settings.R;
import org.lineageos.settings.powertools.PowerProfileTileService;
import org.lineageos.settings.powertools.PowertoolsActivity;
import org.lineageos.settings.saturation.SaturationActivity;
import org.lineageos.settings.saturation.SaturationTileService;

public class ThermalGlobalProfileActivity extends CollapsingToolbarBaseActivity
        implements ExpressiveDesignEnabledProvider {

    @Override
    public boolean isExpressiveDesignEnabled() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null && TileService.ACTION_QS_TILE_PREFERENCES.equals(intent.getAction())) {
            ComponentName comp = intent.getParcelableExtra(
                    Intent.EXTRA_COMPONENT_NAME, ComponentName.class);
            if (comp != null) {
                String cls = comp.getClassName();
                if (SaturationTileService.class.getName().equals(cls)) {
                    startActivity(new Intent(this, SaturationActivity.class));
                    finish();
                    return;
                }
                if (PowerProfileTileService.class.getName().equals(cls)) {
                    startActivity(new Intent(this, PowertoolsActivity.class));
                    finish();
                    return;
                }
            }
        }

        setTitle(R.string.thermal_global_title);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                            new ThermalGlobalProfileFragment(),
                            "thermal_global_profile")
                    .commit();
        }
    }
}
