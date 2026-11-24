/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import org.lineageos.settings.auxcamera.AuxCameraUtils;
import org.lineageos.settings.saturation.SaturationUtils;

public class SummaryProvider extends ContentProvider {

    private static final String KEY_SUMMARY = "com.android.settings.summary";
    private static final String METHOD_THERMAL = "thermalSummary";
    private static final String METHOD_AUX_CAMERA = "auxCameraSummary";
    private static final String METHOD_SATURATION = "saturationSummary";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Context context = getContext();
        if (context == null || method == null) {
            return null;
        }
        String summary;
        switch (method) {
            case METHOD_THERMAL:
                summary = context.getString(R.string.thermal_summary);
                break;
            case METHOD_AUX_CAMERA:
                summary = context.getString(
                        AuxCameraUtils.isEnabled(context) ? R.string.tile_on : R.string.tile_off);
                break;
            case METHOD_SATURATION:
                summary = SaturationUtils.get(context) + "%";
                break;
            default:
                return null;
        }
        Bundle bundle = new Bundle();
        bundle.putString(KEY_SUMMARY, summary);
        return bundle;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
