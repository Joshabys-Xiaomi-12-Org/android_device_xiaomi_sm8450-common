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

package org.lineageos.settings.powertools;

import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import org.lineageos.settings.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

public final class ZramUtils {

    private static final String TAG = "ZramUtils";

    private static final String ZRAM_COMP_ALGO_PATH = "/sys/block/zram0/comp_algorithm";
    private static final String ZRAM_DISKSIZE_PATH = "/sys/block/zram0/disksize";
    private static final String VM_SWAPPINESS_PATH = "/proc/sys/vm/swappiness";
    private static final String VM_VFS_CACHE_PRESSURE_PATH = "/proc/sys/vm/vfs_cache_pressure";
    private static final String VM_PAGE_CLUSTER_PATH = "/proc/sys/vm/page-cluster";
    private static final String VM_DIRTY_RATIO_PATH = "/proc/sys/vm/dirty_ratio";

    private ZramUtils() {}

    public static boolean isZramSupported() {
        return FileUtils.fileExists(ZRAM_DISKSIZE_PATH);
    }

    public static String[] getAvailableAlgorithms() {
        String raw = FileUtils.readOneLine(ZRAM_COMP_ALGO_PATH);
        if (TextUtils.isEmpty(raw)) {
            return new String[0];
        }
        List<String> algos = new ArrayList<>();
        for (String token : raw.trim().split("\\s+")) {
            String clean = token.replace("[", "").replace("]", "").trim();
            if (!clean.isEmpty() && !algos.contains(clean)) {
                algos.add(clean);
            }
        }
        return algos.toArray(new String[0]);
    }

    public static String getCurrentAlgorithm() {
        String raw = FileUtils.readOneLine(ZRAM_COMP_ALGO_PATH);
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        for (String token : raw.trim().split("\\s+")) {
            if (token.startsWith("[") && token.endsWith("]") && token.length() > 2) {
                return token.substring(1, token.length() - 1);
            }
        }
        String[] all = getAvailableAlgorithms();
        return all.length > 0 ? all[0] : null;
    }

    public static String getCurrentSizeMb() {
        String raw = FileUtils.readOneLine(ZRAM_DISKSIZE_PATH);
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        try {
            long bytes = Long.parseLong(raw.trim());
            long mb = bytes / (1024L * 1024L);
            return String.valueOf(mb);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid ZRAM disksize: " + raw, e);
            return null;
        }
    }

    public static String getCurrentSwappiness() {
        String raw = FileUtils.readOneLine(VM_SWAPPINESS_PATH);
        return raw != null ? raw.trim() : null;
    }

    public static String getCurrentVfsCachePressure() {
        String raw = FileUtils.readOneLine(VM_VFS_CACHE_PRESSURE_PATH);
        return raw != null ? raw.trim() : null;
    }

    public static String getCurrentPageCluster() {
        String raw = FileUtils.readOneLine(VM_PAGE_CLUSTER_PATH);
        return raw != null ? raw.trim() : null;
    }

    public static String getCurrentDirtyProfile() {
        String raw = FileUtils.readOneLine(VM_DIRTY_RATIO_PATH);
        if (TextUtils.isEmpty(raw)) {
            return "default";
        }
        try {
            int ratio = Integer.parseInt(raw.trim());
            if (ratio <= 12) return "low";
            if (ratio <= 25) return "default";
            if (ratio <= 35) return "balanced";
            return "throughput";
        } catch (NumberFormatException ignored) {
            return "default";
        }
    }

    public static void applyZram(String sizeMb, String algorithm) {
        if (!TextUtils.isEmpty(sizeMb)) {
            SystemProperties.set("sys.parts.zram.size_mb", sizeMb);
        }
        if (!TextUtils.isEmpty(algorithm)) {
            SystemProperties.set("sys.parts.zram.algorithm", algorithm);
        }
        SystemProperties.set("sys.parts.zram.action", "apply");
    }

    public static void setSwappiness(String value) {
        if (!TextUtils.isEmpty(value)) {
            SystemProperties.set("sys.parts.vm.swappiness", value);
        }
    }

    public static void setVfsCachePressure(String value) {
        if (!TextUtils.isEmpty(value)) {
            SystemProperties.set("sys.parts.vm.vfs_cache_pressure", value);
        }
    }

    public static void setPageCluster(String value) {
        if (!TextUtils.isEmpty(value)) {
            SystemProperties.set("sys.parts.vm.page_cluster", value);
        }
    }

    public static void setDirtyProfile(String profile) {
        if (TextUtils.isEmpty(profile)) {
            return;
        }
        switch (profile) {
            case "low" -> {
                SystemProperties.set("sys.parts.vm.dirty_ratio", "10");
                SystemProperties.set("sys.parts.vm.dirty_background_ratio", "5");
            }
            case "balanced" -> {
                SystemProperties.set("sys.parts.vm.dirty_ratio", "30");
                SystemProperties.set("sys.parts.vm.dirty_background_ratio", "15");
            }
            case "throughput" -> {
                SystemProperties.set("sys.parts.vm.dirty_ratio", "40");
                SystemProperties.set("sys.parts.vm.dirty_background_ratio", "20");
            }
            default -> {
                SystemProperties.set("sys.parts.vm.dirty_ratio", "20");
                SystemProperties.set("sys.parts.vm.dirty_background_ratio", "10");
            }
        }
    }
}
