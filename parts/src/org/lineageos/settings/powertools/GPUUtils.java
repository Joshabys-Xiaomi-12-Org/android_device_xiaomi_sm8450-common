/*
 * Copyright (C) 2025 kenway214
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

import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import org.lineageos.settings.utils.FileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GPUUtils {

    private static final String TAG = "GPUUtils";

    private static final String GPU_AVAILABLE_FREQS_PATH =
            "/sys/class/kgsl/kgsl-3d0/gpu_available_frequencies";
    private static final String GPU_MIN_FREQ_PATH =
            "/sys/class/kgsl/kgsl-3d0/devfreq/min_freq";
    private static final String GPU_MAX_FREQ_PATH =
            "/sys/class/kgsl/kgsl-3d0/devfreq/max_freq";
    private static final String GPU_AVAILABLE_GOVS_PATH =
            "/sys/class/kgsl/kgsl-3d0/devfreq/available_governors";
    private static final String GPU_GOVERNOR_PATH =
            "/sys/class/kgsl/kgsl-3d0/devfreq/governor";
    private static final String GPU_IDLE_TIMER_PATH =
            "/sys/class/kgsl/kgsl-3d0/idle_timer";
    private static final String GPU_CUR_CLK_PATH =
            "/sys/class/kgsl/kgsl-3d0/gpuclk";
    private static final String GPU_CUR_CLK_MHZ_PATH =
            "/sys/class/kgsl/kgsl-3d0/clock_mhz";
    private static final String ADRENO_IDLER_KGSL_PATH =
            "/sys/module/msm_kgsl/parameters/adreno_idler_active";
    private static final String ADRENO_IDLER_STANDALONE_PATH =
            "/sys/module/adreno_idler/parameters/adreno_idler_active";

    public static String[] getAvailableFrequencies() {
        String raw = FileUtils.readOneLine(GPU_AVAILABLE_FREQS_PATH);
        if (TextUtils.isEmpty(raw)) {
            return new String[0];
        }
        List<Long> freqs = new ArrayList<>();
        for (String token : raw.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            try {
                long hz = Long.parseLong(token);
                if (hz > 0 && !freqs.contains(hz)) {
                    freqs.add(hz);
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Ignoring invalid GPU frequency token: " + token);
            }
        }
        Collections.sort(freqs);
        String[] result = new String[freqs.size()];
        for (int i = 0; i < freqs.size(); i++) {
            result[i] = String.valueOf(freqs.get(i));
        }
        return result;
    }

    public static String[] getAvailableGovernors() {
        String raw = FileUtils.readOneLine(GPU_AVAILABLE_GOVS_PATH);
        if (TextUtils.isEmpty(raw)) {
            return new String[0];
        }
        List<String> govs = new ArrayList<>();
        for (String token : raw.trim().split("\\s+")) {
            if (!token.isEmpty() && !govs.contains(token)) {
                govs.add(token);
            }
        }
        return govs.toArray(new String[0]);
    }

    public static String getCurrentMinFrequency() {
        String raw = FileUtils.readOneLine(GPU_MIN_FREQ_PATH);
        return raw != null ? raw.trim() : null;
    }

    public static String getCurrentMaxFrequency() {
        String raw = FileUtils.readOneLine(GPU_MAX_FREQ_PATH);
        return raw != null ? raw.trim() : null;
    }

    public static String getCurrentGovernor() {
        String raw = FileUtils.readOneLine(GPU_GOVERNOR_PATH);
        return raw != null ? raw.trim() : null;
    }

    public static String getCurrentIdleTimer() {
        String raw = FileUtils.readOneLine(GPU_IDLE_TIMER_PATH);
        return raw != null ? raw.trim() : null;
    }

    public static int getCurrentClockMhz() {
        String rawMhz = FileUtils.readOneLine(GPU_CUR_CLK_MHZ_PATH);
        if (!TextUtils.isEmpty(rawMhz)) {
            try {
                return Integer.parseInt(rawMhz.trim());
            } catch (NumberFormatException ignored) {
                // Fall through to Hz node.
            }
        }
        String rawHz = FileUtils.readOneLine(GPU_CUR_CLK_PATH);
        if (!TextUtils.isEmpty(rawHz)) {
            try {
                return (int) (Long.parseLong(rawHz.trim()) / 1_000_000L);
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    public static boolean isAdrenoIdlerAvailable() {
        return FileUtils.fileExists(ADRENO_IDLER_KGSL_PATH)
                || FileUtils.fileExists(ADRENO_IDLER_STANDALONE_PATH);
    }

    public static boolean isAdrenoIdlerEnabled() {
        String path = FileUtils.fileExists(ADRENO_IDLER_KGSL_PATH)
                ? ADRENO_IDLER_KGSL_PATH
                : ADRENO_IDLER_STANDALONE_PATH;
        String raw = FileUtils.readOneLine(path);
        if (TextUtils.isEmpty(raw)) {
            return false;
        }
        String trimmed = raw.trim();
        return "Y".equalsIgnoreCase(trimmed) || "1".equals(trimmed);
    }

    public static String getBatteryMaxFrequency() {
        String[] freqs = getAvailableFrequencies();
        if (freqs.length == 0) {
            return null;
        }
        int idx = Math.max(0, (freqs.length - 1) * 60 / 100);
        return freqs[idx];
    }

    public static void setGPUMinFrequency(String frequencyHz) {
        if (TextUtils.isEmpty(frequencyHz)) {
            return;
        }
        try {
            long freqLong = Long.parseLong(frequencyHz);
            SystemProperties.set("sys.parts.gpu.min_clock",
                    String.valueOf(freqLong / 1000000));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid GPU min frequency: " + frequencyHz, e);
        }
        SystemProperties.set("sys.parts.gpu.min_frequency", frequencyHz);
    }

    public static void setGPUMaxFrequency(String frequencyHz) {
        if (TextUtils.isEmpty(frequencyHz)) {
            return;
        }
        try {
            long freqLong = Long.parseLong(frequencyHz);
            SystemProperties.set("sys.parts.gpu.max_clock",
                    String.valueOf(freqLong / 1000000));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid GPU max frequency: " + frequencyHz, e);
        }
        SystemProperties.set("sys.parts.gpu.max_frequency", frequencyHz);
    }

    public static void setGPUGovernor(String governor) {
        if (!TextUtils.isEmpty(governor)) {
            SystemProperties.set("sys.parts.gpu.governor", governor);
        }
    }

    public static void setAdrenoIdler(boolean enabled) {
        SystemProperties.set("sys.parts.gpu.adreno_idler", enabled ? "Y" : "N");
    }

    public static void setGPUIdleTimer(String timerMs) {
        if (!TextUtils.isEmpty(timerMs)) {
            SystemProperties.set("sys.parts.gpu.idle_timer", timerMs);
        }
    }
}
