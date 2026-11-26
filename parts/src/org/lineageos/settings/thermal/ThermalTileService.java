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
import android.content.Context;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import org.lineageos.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Quick Settings Tile for switching the global Xiaomi thermal profile.
 * Tap cycles through all available profiles in the same order as the profile
 * picker menu; long-press opens ThermalGlobalProfileActivity.
 */
public class ThermalTileService extends TileService {

    private ThermalUtils mThermalUtils;
    private Toast mToast;

    public static void requestUpdate(Context context) {
        requestListeningState(context, new ComponentName(context, ThermalTileService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mThermalUtils = new ThermalUtils(this);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (ThermalUtils.isThermalDisabled()) {
            updateTile();
            return;
        }
        if (mThermalUtils == null) {
            mThermalUtils = new ThermalUtils(this);
        }
        List<Integer> cycle = getAvailableCycleProfiles();
        int current = mThermalUtils.getGlobalProfile();
        int idx = cycle.indexOf(current);
        int next = cycle.get((idx + 1) % cycle.size());
        mThermalUtils.setGlobalProfile(next);
        updateTile();
        showProfileToast(next);
    }

    private void showProfileToast(int state) {
        if (mToast != null) {
            mToast.cancel();
        }
        String message = getString(R.string.thermal_global_tile_label) + ": "
                + getString(ThermalUtils.getProfileNameRes(state));
        mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private List<Integer> getAvailableCycleProfiles() {
        List<Integer> list = new ArrayList<>();
        for (int state = 0; state < ThermalUtils.PROFILE_COUNT; state++) {
            if (ThermalUtils.isProfileAvailable(state)) {
                list.add(state);
            }
        }
        if (list.isEmpty()) {
            list.add(ThermalUtils.STATE_DEFAULT);
        }
        return list;
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        if (mThermalUtils == null) {
            mThermalUtils = new ThermalUtils(this);
        }
        int current = mThermalUtils.getGlobalProfile();
        tile.setLabel(getString(R.string.thermal_global_tile_label));
        if (ThermalUtils.isThermalDisabled()) {
            tile.setSubtitle(getString(R.string.powertools_zram_disabled));
            tile.setState(Tile.STATE_UNAVAILABLE);
        } else {
            tile.setSubtitle(getString(ThermalUtils.getProfileNameRes(current)));
            tile.setState(current == ThermalUtils.STATE_DEFAULT
                    ? Tile.STATE_INACTIVE
                    : Tile.STATE_ACTIVE);
        }
        tile.updateTile();
    }
}
