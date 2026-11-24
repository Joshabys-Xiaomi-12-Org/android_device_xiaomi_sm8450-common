/*
 * Copyright (C) 2024 The LineageOS Project
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

package org.lineageos.settings.saturation;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;

public final class SaturationUtils {

    private static final String TAG = "SaturationUtils";
    private static final int DEFAULT_SATURATION = 100;

    private SaturationUtils() {
    }

    public static int get(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPrefs.getInt(Constants.KEY_SATURATION, DEFAULT_SATURATION);
    }

    public static void apply(int value) {
        IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
        if (surfaceFlinger == null) {
            return;
        }
        float saturation = (value == 0) ? 0.01f : value / 100.0f;
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken("android.ui.ISurfaceComposer");
        data.writeFloat(saturation);
        try {
            surfaceFlinger.transact(1022, data, null, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set saturation", e);
        } finally {
            data.recycle();
        }
    }

    public static void restore(Context context) {
        apply(get(context));
    }
}
