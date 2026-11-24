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
 * codes adapt automatically across different Xiaomi devices.
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

    private ThermalMapResolver() {
        mMapEntries = loadThermalMap();

        mDefaultCode = resolveCode("0", new String[]{"normal"}, false);
        mBenchmarkCode = resolveCode("10", new String[]{"nolimits", "benchmark"}, false);
        mBrowserCode = resolveCode("11", new String[]{"class0", "browser"}, false);
        mCameraCode = resolveCode("12", new String[]{"camera"}, false);
        mDialerCode = resolveCode("8", new String[]{"phone", "incall", "dialer"}, false);
        mGamingCode = resolveCode("13", new String[]{"tgame", "mgame", "game"}, false);
        mStreamingCode = resolveCode("14", new String[]{"youtube", "video", "stream"}, false);
        mPerformanceCode = resolveCode("20", new String[]{"mgame", "nolimits", "per-normal"}, false);
        mBatterySaverCode = resolveCode("3", new String[]{"huanji", "powersave", "lowpower"}, false);

        Log.i(TAG, "Resolved thermal sconfig codes (" + mMapEntries.size() + " map entries): "
                + "default=" + mDefaultCode
                + " benchmark=" + mBenchmarkCode
                + " browser=" + mBrowserCode
                + " camera=" + mCameraCode
                + " dialer=" + mDialerCode
                + " gaming=" + mGamingCode
                + " streaming=" + mStreamingCode
                + " performance=" + mPerformanceCode
                + " batterySaver=" + mBatterySaverCode);
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

    public String getBenchmarkCode() {
        return mBenchmarkCode;
    }

    public String getBrowserCode() {
        return mBrowserCode;
    }

    public String getCameraCode() {
        return mCameraCode;
    }

    public String getDialerCode() {
        return mDialerCode;
    }

    public String getGamingCode() {
        return mGamingCode;
    }

    public String getStreamingCode() {
        return mStreamingCode;
    }

    public String getPerformanceCode() {
        return mPerformanceCode;
    }

    public String getBatterySaverCode() {
        return mBatterySaverCode;
    }

    private String resolveCode(String fallback, String[] keywords, boolean allowExtended) {
        if (mMapEntries.isEmpty()) {
            return fallback;
        }
        // First pass: base thermal codes (< 30, standard mi_thermald scene range)
        String match = findMatch(keywords, 30);
        if (match != null) {
            return match;
        }
        // Second pass: extended codes if requested or not found in base range
        if (allowExtended || match == null) {
            match = findMatch(keywords, Integer.MAX_VALUE);
            if (match != null) {
                return match;
            }
        }
        return fallback;
    }

    private String findMatch(String[] keywords, int maxCodeExclusive) {
        for (String kw : keywords) {
            for (Map.Entry<Integer, String> entry : mMapEntries.entrySet()) {
                int code = entry.getKey();
                if (code >= maxCodeExclusive) {
                    continue;
                }
                String conf = entry.getValue().toLowerCase(Locale.ROOT);
                if (conf.contains("unfold") || conf.contains("iec") || conf.contains("videochat")) {
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
