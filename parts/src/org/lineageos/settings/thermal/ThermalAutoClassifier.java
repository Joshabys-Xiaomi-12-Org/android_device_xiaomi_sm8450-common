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
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automatically classifies installed apps into the most suitable Xiaomi
 * thermal profile using Android's ApplicationInfo category, declared intents
 * (camera, dialer, browser), and well-known package patterns.
 */
public final class ThermalAutoClassifier {

    private static final String[] BENCHMARK_KEYWORDS = {
            "antutu", "geekbench", "3dmark", "pcmark", "gfxbench",
            "throttling", "burnout", "aida64", "benchmark", "wildlife"
    };

    private static final String[] EXTREME_GAME_KEYWORDS = {
            "genshin", "wutheringwaves", "honkai", "hkrpg", "zenless",
            "nap.global", "warzone", "fortnite", "arkmobile", "carx"
    };

    private static final String[] HEAVY_GAME_KEYWORDS = {
            "pubg", "ig.pubg", "codm", "callofduty", "wildrift", "mobile.legends",
            "mlbb", "freefire", "apexlegends", "blackdesert", "asphalt",
            "diablo", "minecraft", "roblox", "ea.gp.fifa", "efootball"
    };

    private static final String[] STREAMING_KEYWORDS = {
            "youtube", "netflix", "twitch", "disney", "primevideo", "avod",
            "hbomax", "wbd.stream", "crunchyroll", "globoplay", "paramount",
            "starplus", "tiktok", "musically", "kick.mobile"
    };

    private static final String[] VIDEO_CALL_KEYWORDS = {
            "zoom.us", "videomeetings", "teams", "tachyon", "meetings",
            "skype", "discord", "webex"
    };

    private static final String[] NAVIGATION_KEYWORDS = {
            "apps.maps", "waze", "osmand", "here.app.maps", "tomtom", "sygic"
    };

    private static final String[] VIDEO_PLAYER_KEYWORDS = {
            "videolan.vlc", "mxtech.videoplayer", "novavideoplayer", "mpv.android"
    };

    private static final String[] CAMERA_KEYWORDS = {
            "camera", "gcam", "opencamera", "com.agc.", "com.lmc.", "bsg."
    };

    private static final String[] DIALER_KEYWORDS = {
            "dialer", "incallui", "android.phone", "truecaller"
    };

    private static final String[] BROWSER_KEYWORDS = {
            "chrome", "firefox", "fenix", "brave", "emmx", "opera",
            "vivaldi", "duckduckgo", "kiwibrowser", "mark.via", "browser"
    };

    private final Context mContext;
    private final Map<String, Integer> mCache = new ConcurrentHashMap<>();
    private volatile Set<String> mCameraPackages;
    private volatile Set<String> mDialerPackages;
    private volatile Set<String> mBrowserPackages;

    public ThermalAutoClassifier(Context context) {
        mContext = context.getApplicationContext();
    }

    public void clearCache() {
        mCache.clear();
        mCameraPackages = null;
        mDialerPackages = null;
        mBrowserPackages = null;
    }

    public int classify(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return ThermalUtils.STATE_DEFAULT;
        }
        Integer cached = mCache.get(packageName);
        if (cached != null) {
            return cached;
        }
        int detected = detectProfile(packageName);
        mCache.put(packageName, detected);
        return detected;
    }

    private int detectProfile(String packageName) {
        String pkg = packageName.toLowerCase(Locale.ROOT);

        // 1. Benchmarks
        if (matchesAny(pkg, BENCHMARK_KEYWORDS)) {
            return ThermalUtils.STATE_BENCHMARK;
        }

        // 2. Known extreme / heavy games
        if (matchesAny(pkg, EXTREME_GAME_KEYWORDS)) {
            return pickAvailable(
                    ThermalUtils.STATE_EXTREME_GAMING,
                    ThermalUtils.STATE_HEAVY_GAMING,
                    ThermalUtils.STATE_GAMING);
        }
        if (matchesAny(pkg, HEAVY_GAME_KEYWORDS)) {
            return pickAvailable(
                    ThermalUtils.STATE_HEAVY_GAMING,
                    ThermalUtils.STATE_GAMING);
        }

        // 3. Android ApplicationInfo category & game flag
        PackageManager pm = mContext.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            boolean isGame = ai.category == ApplicationInfo.CATEGORY_GAME
                    || (ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0;
            if (isGame) {
                return ThermalUtils.STATE_GAMING;
            }
            if (ai.category == ApplicationInfo.CATEGORY_MAPS) {
                return pickAvailable(ThermalUtils.STATE_NAVIGATION, ThermalUtils.STATE_DEFAULT);
            }
            if (ai.category == ApplicationInfo.CATEGORY_VIDEO) {
                if (matchesAny(pkg, VIDEO_PLAYER_KEYWORDS)) {
                    return pickAvailable(
                            ThermalUtils.STATE_VIDEO_PLAYBACK,
                            ThermalUtils.STATE_STREAMING);
                }
                return ThermalUtils.STATE_STREAMING;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // Fall through to intent/keyword matching.
        }

        // 4. Camera apps (intent capability or keyword)
        if (getCameraPackages(pm).contains(packageName) || matchesAny(pkg, CAMERA_KEYWORDS)) {
            return ThermalUtils.STATE_CAMERA;
        }

        // 5. Dialer apps
        if (getDialerPackages(pm).contains(packageName) || matchesAny(pkg, DIALER_KEYWORDS)) {
            return ThermalUtils.STATE_DIALER;
        }

        // 6. Navigation apps
        if (matchesAny(pkg, NAVIGATION_KEYWORDS)) {
            return pickAvailable(ThermalUtils.STATE_NAVIGATION, ThermalUtils.STATE_DEFAULT);
        }

        // 7. Video call apps
        if (matchesAny(pkg, VIDEO_CALL_KEYWORDS)) {
            return pickAvailable(ThermalUtils.STATE_VIDEO_CALL, ThermalUtils.STATE_DEFAULT);
        }

        // 8. Streaming & local video players
        if (matchesAny(pkg, STREAMING_KEYWORDS)) {
            return ThermalUtils.STATE_STREAMING;
        }
        if (matchesAny(pkg, VIDEO_PLAYER_KEYWORDS)) {
            return pickAvailable(ThermalUtils.STATE_VIDEO_PLAYBACK, ThermalUtils.STATE_STREAMING);
        }

        // 9. Web browsers
        if (getBrowserPackages(pm).contains(packageName) || matchesAny(pkg, BROWSER_KEYWORDS)) {
            return ThermalUtils.STATE_BROWSER;
        }

        return ThermalUtils.STATE_DEFAULT;
    }

    private Set<String> getCameraPackages(PackageManager pm) {
        if (mCameraPackages == null) {
            mCameraPackages = queryIntentPackages(pm, new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
        }
        return mCameraPackages;
    }

    private Set<String> getDialerPackages(PackageManager pm) {
        if (mDialerPackages == null) {
            mDialerPackages = queryIntentPackages(pm, new Intent(Intent.ACTION_DIAL));
        }
        return mDialerPackages;
    }

    private Set<String> getBrowserPackages(PackageManager pm) {
        if (mBrowserPackages == null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            mBrowserPackages = queryIntentPackages(pm, intent);
        }
        return mBrowserPackages;
    }

    private static Set<String> queryIntentPackages(PackageManager pm, Intent intent) {
        Set<String> pkgs = new HashSet<>();
        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        if (list != null) {
            for (ResolveInfo info : list) {
                if (info.activityInfo != null && info.activityInfo.packageName != null) {
                    pkgs.add(info.activityInfo.packageName);
                }
            }
        }
        return pkgs;
    }

    private static boolean matchesAny(String pkg, String[] keywords) {
        for (String kw : keywords) {
            if (pkg.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static int pickAvailable(int... candidates) {
        for (int state : candidates) {
            if (ThermalUtils.isProfileAvailable(state)) {
                return state;
            }
        }
        return ThermalUtils.STATE_DEFAULT;
    }
}
