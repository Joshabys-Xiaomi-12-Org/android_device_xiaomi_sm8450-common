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

    public static String getCurrentMinFrequency() {
        String raw = FileUtils.readOneLine(GPU_MIN_FREQ_PATH);
        return raw != null ? raw.trim() : null;
    }

    public static String getCurrentMaxFrequency() {
        String raw = FileUtils.readOneLine(GPU_MAX_FREQ_PATH);
        return raw != null ? raw.trim() : null;
    }

    public static void setGPUMinFrequency(String frequencyHz) {
        SystemProperties.set("persist.sys.parts.gpu.min_frequency", frequencyHz);
        try {
            long freqLong = Long.parseLong(frequencyHz);
            SystemProperties.set("persist.sys.parts.gpu.min_clock",
                    String.valueOf(freqLong / 1000000));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid GPU min frequency: " + frequencyHz, e);
        }
    }

    public static void setGPUMaxFrequency(String frequencyHz) {
        SystemProperties.set("persist.sys.parts.gpu.max_frequency", frequencyHz);
        try {
            long freqLong = Long.parseLong(frequencyHz);
            SystemProperties.set("persist.sys.parts.gpu.max_clock",
                    String.valueOf(freqLong / 1000000));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid GPU max frequency: " + frequencyHz, e);
        }
    }
}
