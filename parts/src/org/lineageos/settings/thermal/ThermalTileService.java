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

import org.lineageos.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Quick Settings Tile for switching the global Xiaomi thermal profile.
 * Tap cycles through key available profiles; long-press opens the full
 * profile picker menu (ThermalGlobalProfileActivity).
 */
public class ThermalTileService extends TileService {

    private static final int[] QUICK_CYCLE_ORDER = {
            ThermalUtils.STATE_DEFAULT,
            ThermalUtils.STATE_COOL,
            ThermalUtils.STATE_GAMING,
            ThermalUtils.STATE_HEAVY_GAMING,
            ThermalUtils.STATE_EXTREME_GAMING,
            ThermalUtils.STATE_BENCHMARK
    };

    private ThermalUtils mThermalUtils;

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
        if (mThermalUtils == null) {
            mThermalUtils = new ThermalUtils(this);
        }
        List<Integer> cycle = getAvailableCycleProfiles();
        int current = mThermalUtils.getGlobalProfile();
        int idx = cycle.indexOf(current);
        int next = cycle.get((idx + 1) % cycle.size());
        mThermalUtils.setGlobalProfile(next);
        updateTile();
    }

    private List<Integer> getAvailableCycleProfiles() {
        List<Integer> list = new ArrayList<>();
        for (int state : QUICK_CYCLE_ORDER) {
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
        tile.setSubtitle(getString(ThermalUtils.getProfileNameRes(current)));
        tile.setState(current == ThermalUtils.STATE_DEFAULT
                ? Tile.STATE_INACTIVE
                : Tile.STATE_ACTIVE);
        tile.updateTile();
    }
}
