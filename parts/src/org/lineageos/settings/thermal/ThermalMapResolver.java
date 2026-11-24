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

import android.util.Log;

import org.lineageos.settings.thermaleditor.MiThermalCrypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dynamically resolves Xiaomi mi_thermald sconfig profile codes by decrypting
 * and parsing /vendor/etc/thermal-map.conf (or thermal-*-map.conf), so profile
 * codes and available profiles adapt automatically across Xiaomi devices.
 */
public final class ThermalMapResolver {

    private static final String TAG = "ThermalMapResolver";
    private static final String VENDOR_ETC = "/vendor/etc";
    private static final Pattern ENTRY_PATTERN =
            Pattern.compile("^\\[(\\d+):([^\\]]+)\\]$");

    private static volatile ThermalMapResolver sInstance;

    private final Map<Integer, String> mMapEntries;

    private final String mDefaultCode;
    private final String mBenchmarkCode;
    private final String mBrowserCode;
    private final String mCameraCode;
    private final String mDialerCode;
    private final String mGamingCode;
    private final String mStreamingCode;
    private final String mPerformanceCode;
    private final String mBatterySaverCode;

    // Optional profiles auto-detected from thermal-map.conf (null if unsupported)
    private final String mHeavyGamingCode;
    private final String mExtremeGamingCode;
    private final String mNavigationCode;
    private final String mVideoCallCode;
    private final String mVideoPlaybackCode;
    private final String m4kRecordingCode;
    private final String mArVrCode;
    private final String mCoolCode;

    private ThermalMapResolver() {
        mMapEntries = loadThermalMap();

        mDefaultCode = resolveCode("0", new String[]{"normal"}, false);
        mBenchmarkCode = resolveCode("10", new String[]{"nolimits", "benchmark"}, false);
        mBrowserCode = resolveCode("11", new String[]{"class0", "browser"}, false);
        mCameraCode = resolveCode("12", new String[]{"camera"}, false);
        mDialerCode = resolveCode("8", new String[]{"phone", "incall", "dialer"}, false);
        mGamingCode = resolveCode("13", new String[]{"tgame", "mgame", "game"}, false);
        mStreamingCode = resolveCode("14", new String[]{"youtube", "stream"}, false);
        mPerformanceCode = resolveCode("20", new String[]{"mgame", "nolimits", "per-normal"}, false);
        mBatterySaverCode = resolveCode("3", new String[]{"huanji", "powersave", "lowpower"}, false);

        mHeavyGamingCode = resolveOptional(new String[]{"mgame"}, false);
        mExtremeGamingCode = resolveOptional(new String[]{"yuanshen"}, false);
        mNavigationCode = resolveOptional(new String[]{"navigation"}, false);
        mVideoCallCode = resolveOptional(new String[]{"videochat"}, true);
        mVideoPlaybackCode = resolveOptional(new String[]{"-video.conf"}, false);
        m4kRecordingCode = resolveOptional(new String[]{"4k", "8k"}, false);
        mArVrCode = resolveOptional(new String[]{"arvr"}, false);
        mCoolCode = resolveOptional(new String[]{"huanji", "powersave", "lowpower"}, false);

        Log.i(TAG, "Resolved thermal sconfig codes (" + mMapEntries.size() + " map entries): "
                + "default=" + mDefaultCode
                + " benchmark=" + mBenchmarkCode
                + " browser=" + mBrowserCode
                + " camera=" + mCameraCode
                + " dialer=" + mDialerCode
                + " gaming=" + mGamingCode
                + " streaming=" + mStreamingCode
                + " heavyGaming=" + mHeavyGamingCode
                + " extremeGaming=" + mExtremeGamingCode
                + " navigation=" + mNavigationCode
                + " videoCall=" + mVideoCallCode
                + " videoPlayback=" + mVideoPlaybackCode
                + " 4k=" + m4kRecordingCode
                + " arvr=" + mArVrCode
                + " cool=" + mCoolCode);
    }

    public static ThermalMapResolver getInstance() {
        if (sInstance == null) {
            synchronized (ThermalMapResolver.class) {
                if (sInstance == null) {
                    sInstance = new ThermalMapResolver();
                }
            }
        }
        return sInstance;
    }

    public String getDefaultCode() {
        return mDefaultCode;
    }

    public String getPerformanceCode() {
        return mPerformanceCode;
    }

    public String getGamingCode() {
        return mGamingCode;
    }

    public String getBatterySaverCode() {
        return mBatterySaverCode;
    }

    public String getCodeForState(int state) {
        return switch (state) {
            case ThermalUtils.STATE_BENCHMARK -> mBenchmarkCode;
            case ThermalUtils.STATE_BROWSER -> mBrowserCode;
            case ThermalUtils.STATE_CAMERA -> mCameraCode;
            case ThermalUtils.STATE_DIALER -> mDialerCode;
            case ThermalUtils.STATE_GAMING -> mGamingCode;
            case ThermalUtils.STATE_STREAMING -> mStreamingCode;
            case ThermalUtils.STATE_HEAVY_GAMING ->
                    mHeavyGamingCode != null ? mHeavyGamingCode : mGamingCode;
            case ThermalUtils.STATE_EXTREME_GAMING ->
                    mExtremeGamingCode != null ? mExtremeGamingCode : mGamingCode;
            case ThermalUtils.STATE_NAVIGATION ->
                    mNavigationCode != null ? mNavigationCode : mDefaultCode;
            case ThermalUtils.STATE_VIDEO_CALL ->
                    mVideoCallCode != null ? mVideoCallCode : mDefaultCode;
            case ThermalUtils.STATE_VIDEO_PLAYBACK ->
                    mVideoPlaybackCode != null ? mVideoPlaybackCode : mStreamingCode;
            case ThermalUtils.STATE_4K_RECORDING ->
                    m4kRecordingCode != null ? m4kRecordingCode : mCameraCode;
            case ThermalUtils.STATE_ARVR ->
                    mArVrCode != null ? mArVrCode : mGamingCode;
            case ThermalUtils.STATE_COOL ->
                    mCoolCode != null ? mCoolCode : mDefaultCode;
            default -> mDefaultCode;
        };
    }

    public boolean isProfileAvailable(int state) {
        return switch (state) {
            case ThermalUtils.STATE_DEFAULT,
                 ThermalUtils.STATE_BENCHMARK,
                 ThermalUtils.STATE_BROWSER,
                 ThermalUtils.STATE_CAMERA,
                 ThermalUtils.STATE_DIALER,
                 ThermalUtils.STATE_GAMING,
                 ThermalUtils.STATE_STREAMING -> true;
            case ThermalUtils.STATE_HEAVY_GAMING ->
                    mHeavyGamingCode != null && !mHeavyGamingCode.equals(mGamingCode);
            case ThermalUtils.STATE_EXTREME_GAMING -> mExtremeGamingCode != null;
            case ThermalUtils.STATE_NAVIGATION -> mNavigationCode != null;
            case ThermalUtils.STATE_VIDEO_CALL -> mVideoCallCode != null;
            case ThermalUtils.STATE_VIDEO_PLAYBACK -> mVideoPlaybackCode != null;
            case ThermalUtils.STATE_4K_RECORDING -> m4kRecordingCode != null;
            case ThermalUtils.STATE_ARVR -> mArVrCode != null;
            case ThermalUtils.STATE_COOL -> mCoolCode != null;
            default -> false;
        };
    }

    private String resolveCode(String fallback, String[] keywords, boolean allowVideoChat) {
        String match = resolveOptional(keywords, allowVideoChat);
        return match != null ? match : fallback;
    }

    private String resolveOptional(String[] keywords, boolean allowVideoChat) {
        if (mMapEntries.isEmpty()) {
            return null;
        }
        String match = findMatch(keywords, 30, allowVideoChat);
        if (match != null) {
            return match;
        }
        return findMatch(keywords, Integer.MAX_VALUE, allowVideoChat);
    }

    private String findMatch(String[] keywords, int maxCodeExclusive, boolean allowVideoChat) {
        for (String kw : keywords) {
            for (Map.Entry<Integer, String> entry : mMapEntries.entrySet()) {
                int code = entry.getKey();
                if (code >= maxCodeExclusive) {
                    continue;
                }
                String conf = entry.getValue().toLowerCase(Locale.ROOT);
                if (conf.contains("unfold") || conf.contains("iec")) {
                    continue;
                }
                if (!allowVideoChat && conf.contains("videochat")) {
                    continue;
                }
                if (conf.contains(kw)) {
                    return String.valueOf(code);
                }
            }
        }
        return null;
    }

    private static Map<Integer, String> loadThermalMap() {
        File mapFile = findThermalMapFile();
        if (mapFile == null) {
            Log.w(TAG, "No thermal-map.conf found in " + VENDOR_ETC + ", using fallback codes");
            return Collections.emptyMap();
        }
        try {
            String text = readAndDecrypt(mapFile);
            Map<Integer, String> result = new TreeMap<>();
            for (String rawLine : text.split("\\r?\\n")) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }
                Matcher m = ENTRY_PATTERN.matcher(line);
                if (m.matches()) {
                    try {
                        int code = Integer.parseInt(m.group(1));
                        String conf = m.group(2).trim();
                        result.put(code, conf);
                    } catch (NumberFormatException ignored) {
                        // Skip malformed code.
                    }
                }
            }
            Log.i(TAG, "Loaded " + result.size() + " thermal map entries from " + mapFile.getName());
            return result;
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse " + mapFile.getAbsolutePath() + ", using fallback codes", e);
            return Collections.emptyMap();
        }
    }

    private static File findThermalMapFile() {
        File primary = new File(VENDOR_ETC, "thermal-map.conf");
        if (primary.isFile() && primary.canRead()) {
            return primary;
        }
        File dir = new File(VENDOR_ETC);
        File[] candidates = dir.listFiles((d, name) ->
                name.startsWith("thermal-")
                        && name.endsWith("-map.conf")
                        && !name.contains("region"));
        if (candidates != null && candidates.length > 0) {
            Arrays.sort(candidates);
            return candidates[0];
        }
        return null;
    }

    private static String readAndDecrypt(File file) throws IOException, GeneralSecurityException {
        byte[] raw;
        try (FileInputStream fis = new FileInputStream(file)) {
            raw = new byte[(int) file.length()];
            int offset = 0;
            while (offset < raw.length) {
                int read = fis.read(raw, offset, raw.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            if (offset != raw.length) {
                raw = Arrays.copyOf(raw, offset);
            }
        }
        if (looksLikePlainText(raw)) {
            return new String(raw, StandardCharsets.UTF_8);
        }
        return new String(MiThermalCrypt.decrypt(raw), StandardCharsets.UTF_8);
    }

    private static boolean looksLikePlainText(byte[] data) {
        if (data.length == 0) {
            return true;
        }
        int checkLen = Math.min(data.length, 256);
        int printable = 0;
        for (int i = 0; i < checkLen; i++) {
            int b = data[i] & 0xFF;
            if (b == 0x09 || b == 0x0A || b == 0x0D || (b >= 0x20 && b <= 0x7E)) {
                printable++;
            }
        }
        return printable >= (checkLen * 9) / 10;
    }
}
