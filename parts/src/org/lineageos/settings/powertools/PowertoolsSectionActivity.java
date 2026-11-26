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

package org.lineageos.settings.powertools;

import android.os.Bundle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.ExpressiveDesignEnabledProvider;

import org.lineageos.settings.R;

public class PowertoolsSectionActivity extends CollapsingToolbarBaseActivity
        implements ExpressiveDesignEnabledProvider {

    public static final String EXTRA_SECTION = "extra_section";
    public static final String EXTRA_TITLE_RES = "extra_title_res";

    @Override
    public boolean isExpressiveDesignEnabled() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int section = getIntent().getIntExtra(
                EXTRA_SECTION, PowertoolsSettingsFragment.TAB_CPU);
        int titleRes = getIntent().getIntExtra(
                EXTRA_TITLE_RES, R.string.Powertools_title);
        setTitle(titleRes);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                            PowertoolsSettingsFragment.newInstance(section))
                    .commit();
        }
    }
}
