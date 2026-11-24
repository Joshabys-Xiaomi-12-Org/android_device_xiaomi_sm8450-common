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

public final class CPUUtils {

    private static final String TAG = "CPUUtils";

    public static final int POLICY_LITTLE = 0;
    public static final int POLICY_BIG = 4;
    public static final int POLICY_PRIME = 7;

    private static String policyPath(int policyCpu, String node) {
        return "/sys/devices/system/cpu/cpufreq/policy" + policyCpu + "/" + node;
    }

    public static String[] getAvailableFrequencies(int policyCpu) {
        String raw = FileUtils.readOneLine(policyPath(policyCpu, "scaling_available_frequencies"));
        if (TextUtils.isEmpty(raw)) {
            return new String[0];
        }
        List<Long> freqs = new ArrayList<>();
        for (String token : raw.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            try {
                long khz = Long.parseLong(token);
                if (khz > 0 && !freqs.contains(khz)) {
                    freqs.add(khz);
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Ignoring invalid CPU frequency token: " + token);
            }
        }
        Collections.sort(freqs);
        String[] result = new String[freqs.size()];
        for (int i = 0; i < freqs.size(); i++) {
            result[i] = String.valueOf(freqs.get(i));
        }
        return result;
    }

    public static String[] getAvailableGovernors(int policyCpu) {
        String raw = FileUtils.readOneLine(policyPath(policyCpu, "scaling_available_governors"));
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

    public static String getCurrentMinFrequency(int policyCpu) {
        String raw = FileUtils.readOneLine(policyPath(policyCpu, "scaling_min_freq"));
        return raw != null ? raw.trim() : null;
    }

    public static String getCurrentMaxFrequency(int policyCpu) {
        String raw = FileUtils.readOneLine(policyPath(policyCpu, "scaling_max_freq"));
        return raw != null ? raw.trim() : null;
    }

    public static String getCurrentGovernor(int policyCpu) {
        String raw = FileUtils.readOneLine(policyPath(policyCpu, "scaling_governor"));
        return raw != null ? raw.trim() : null;
    }

    public static String getCappedMaxFrequency(int policyCpu, int percent) {
        String[] freqs = getAvailableFrequencies(policyCpu);
        if (freqs.length == 0) {
            return null;
        }
        int idx = Math.max(0, Math.min(freqs.length - 1, (freqs.length - 1) * percent / 100));
        return freqs[idx];
    }

    public static void setCPULittleFreq(String minFreq, String maxFreq, String governor) {
        if (!TextUtils.isEmpty(minFreq)) {
            SystemProperties.set("sys.parts.cpu.little.min_frequency", minFreq);
        }
        if (!TextUtils.isEmpty(maxFreq)) {
            SystemProperties.set("sys.parts.cpu.little.max_frequency", maxFreq);
        }
        if (!TextUtils.isEmpty(governor)) {
            SystemProperties.set("sys.parts.cpu.little.governor", governor);
        }
    }

    public static void setCPUBigFreq(String minFreq, String maxFreq, String governor) {
        if (!TextUtils.isEmpty(minFreq)) {
            SystemProperties.set("sys.parts.cpu.big.min_frequency", minFreq);
        }
        if (!TextUtils.isEmpty(maxFreq)) {
            SystemProperties.set("sys.parts.cpu.big.max_frequency", maxFreq);
        }
        if (!TextUtils.isEmpty(governor)) {
            SystemProperties.set("sys.parts.cpu.big.governor", governor);
        }
    }

    public static void setCPUPrimeFreq(String minFreq, String maxFreq, String governor) {
        if (!TextUtils.isEmpty(minFreq)) {
            SystemProperties.set("sys.parts.cpu.prime.min_frequency", minFreq);
        }
        if (!TextUtils.isEmpty(maxFreq)) {
            SystemProperties.set("sys.parts.cpu.prime.max_frequency", maxFreq);
        }
        if (!TextUtils.isEmpty(governor)) {
            SystemProperties.set("sys.parts.cpu.prime.governor", governor);
        }
    }

    public static void setInputBoost(String mode) {
        if (!TextUtils.isEmpty(mode)) {
            SystemProperties.set("sys.parts.cpu.input_boost", mode);
        }
    }

    public static void setWaltProfile(String profile) {
        if (!TextUtils.isEmpty(profile)) {
            SystemProperties.set("sys.parts.cpu.walt_profile", profile);
        }
    }

    public static void setUclampProfile(String profile) {
        if (!TextUtils.isEmpty(profile)) {
            SystemProperties.set("sys.parts.cpu.uclamp", profile);
        }
    }
}
