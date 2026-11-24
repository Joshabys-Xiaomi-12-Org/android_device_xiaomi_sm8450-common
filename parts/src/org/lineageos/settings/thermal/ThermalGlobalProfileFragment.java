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

import android.content.Context;
import android.os.Bundle;

import androidx.preference.PreferenceScreen;

import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class ThermalGlobalProfileFragment extends SettingsBasePreferenceFragment {

    private ThermalUtils mThermalUtils;
    private final SelectorWithWidgetPreference[] mOptions =
            new SelectorWithWidgetPreference[ThermalUtils.PROFILE_COUNT];
    private int mCurrentState;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = requireContext();
        mThermalUtils = new ThermalUtils(context);
        mCurrentState = mThermalUtils.getGlobalProfile();

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        for (int i = 0; i < ThermalUtils.PROFILE_COUNT; i++) {
            final int state = i;
            if (!ThermalUtils.isProfileAvailable(state) && state != mCurrentState) {
                continue;
            }
            SelectorWithWidgetPreference pref = new SelectorWithWidgetPreference(context);
            pref.setTitle(ThermalUtils.getProfileNameRes(state));
            pref.setSummary(ThermalUtils.getProfileDescRes(state));
            pref.setPersistent(false);
            pref.setChecked(state == mCurrentState);
            pref.setOnPreferenceClickListener(p -> {
                selectProfile(state);
                return true;
            });
            screen.addPreference(pref);
            mOptions[state] = pref;
        }

        FooterPreference footer = new FooterPreference(context);
        footer.setTitle(R.string.thermal_global_footer);
        footer.setSelectable(false);
        screen.addPreference(footer);

        setPreferenceScreen(screen);
    }

    private void selectProfile(int state) {
        if (state != mCurrentState) {
            mCurrentState = state;
            mThermalUtils.setGlobalProfile(state);
            ThermalTileService.requestUpdate(requireContext());
        }
        for (int i = 0; i < mOptions.length; i++) {
            if (mOptions[i] != null) {
                mOptions[i].setChecked(i == mCurrentState);
            }
        }
    }
}
