/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.auxcamera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AuxCameraUtils {

    private static final String TAG = "AuxCameraUtils";

    private static final String PROP = "persist.sys.aux_camera_control";
    private static final String SETTINGS_KEY = "aux_camera_excluded_apps";
    private static final String ENABLED_KEY = "aux_camera_enabled";

    // SystemProperties.set() rejects longer values for non read-only properties
    private static final int MAX_PROP_VALUE_BYTES = 91;

    // Keep in sync with vendor.camera.aux.packageexcludelist in properties/vendor.prop
    private static final String DEFAULT_EXCLUDED_APPS =
            "com.adobe.scan.android,com.tencent.mm,us.zoom.videomeetings,in.org.npci.upiapp";

    private static volatile Set<String> sCachedApps;

    private AuxCameraUtils() {
    }

    public static boolean isEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(), ENABLED_KEY, 0) == 1;
    }

    public static void setEnabled(Context context, boolean enabled) {
        Settings.System.putInt(context.getContentResolver(), ENABLED_KEY, enabled ? 1 : 0);
        if (enabled) {
            restore(context);
        } else {
            applyProp(DEFAULT_EXCLUDED_APPS);
        }
    }

    public static Set<String> getApps(Context context) {
        Set<String> cached = sCachedApps;
        if (cached != null) {
            return cached;
        }

        String value = Settings.System.getString(context.getContentResolver(), SETTINGS_KEY);
        if (value == null) {
            value = DEFAULT_EXCLUDED_APPS;
        }

        PackageManager pm = context.getPackageManager();
        Set<String> validApps = new LinkedHashSet<>();
        for (String raw : value.split(",")) {
            String pkg = raw.trim();
            if (pkg.isEmpty()) {
                continue;
            }
            try {
                pm.getApplicationInfo(pkg, 0);
                validApps.add(pkg);
            } catch (PackageManager.NameNotFoundException ignored) {
                // Skip uninstalled packages
            }
        }

        String cleaned = TextUtils.join(",", validApps);
        if (!cleaned.equals(value)) {
            Settings.System.putString(context.getContentResolver(), SETTINGS_KEY, cleaned);
            Log.i(TAG, "Cleaned aux camera app list");
        }

        Set<String> unmodifiable = Collections.unmodifiableSet(validApps);
        sCachedApps = unmodifiable;
        return unmodifiable;
    }

    public static boolean isAppEnabled(Context context, String packageName) {
        return getApps(context).contains(packageName);
    }

    public static boolean setAppEnabled(Context context, String packageName, boolean enabled) {
        Set<String> apps = new LinkedHashSet<>(getApps(context));
        if (enabled) {
            apps.add(packageName);
        } else {
            apps.remove(packageName);
        }

        String serialized = TextUtils.join(",", apps);
        if (enabled && serialized.getBytes(StandardCharsets.UTF_8).length > MAX_PROP_VALUE_BYTES) {
            Log.w(TAG, "App list exceeds property limit, not adding " + packageName);
            return false;
        }

        sCachedApps = Collections.unmodifiableSet(apps);
        Settings.System.putString(context.getContentResolver(), SETTINGS_KEY, serialized);

        if (isEnabled(context)) {
            applyProp(serialized.isEmpty() ? "none" : serialized);
        }
        return true;
    }

    public static void restore(Context context) {
        if (!isEnabled(context)) {
            return;
        }
        String serialized = TextUtils.join(",", getApps(context));
        applyProp(serialized.isEmpty() ? "none" : serialized);
    }

    private static void applyProp(String value) {
        try {
            SystemProperties.set(PROP, TextUtils.isEmpty(value) ? "none" : value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set " + PROP, e);
        }
    }
}
