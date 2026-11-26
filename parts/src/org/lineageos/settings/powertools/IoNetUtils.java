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

import org.lineageos.settings.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

public final class IoNetUtils {

    private static final String IO_SCHEDULER_PATH = "/sys/block/sda/queue/scheduler";
    private static final String IO_READ_AHEAD_PATH = "/sys/block/sda/queue/read_ahead_kb";
    private static final String TCP_AVAILABLE_PATH =
            "/proc/sys/net/ipv4/tcp_available_congestion_control";
    private static final String TCP_CURRENT_PATH =
            "/proc/sys/net/ipv4/tcp_congestion_control";

    private static final String[] FALLBACK_SCHEDULERS = {
            "mq-deadline", "kyber", "bfq", "none"
    };

    private static final String[] FALLBACK_TCP_ALGOS = {
            "cubic", "bbr", "reno"
    };

    private IoNetUtils() {}

    public static String[] getAvailableSchedulers() {
        String raw = FileUtils.readOneLine(IO_SCHEDULER_PATH);
        if (TextUtils.isEmpty(raw)) {
            return FALLBACK_SCHEDULERS;
        }
        List<String> list = new ArrayList<>();
        for (String token : raw.trim().split("\\s+")) {
            String clean = token.replace("[", "").replace("]", "").trim();
            if (!clean.isEmpty() && !list.contains(clean)) {
                list.add(clean);
            }
        }
        return list.isEmpty() ? FALLBACK_SCHEDULERS : list.toArray(new String[0]);
    }

    public static String getCurrentScheduler() {
        String raw = FileUtils.readOneLine(IO_SCHEDULER_PATH);
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        for (String token : raw.trim().split("\\s+")) {
            if (token.startsWith("[") && token.endsWith("]") && token.length() > 2) {
                return token.substring(1, token.length() - 1);
            }
        }
        return null;
    }

    public static String getCurrentReadAheadKb() {
        String raw = FileUtils.readOneLine(IO_READ_AHEAD_PATH);
        return raw != null ? raw.trim() : null;
    }

    public static String[] getAvailableTcpAlgorithms() {
        String raw = FileUtils.readOneLine(TCP_AVAILABLE_PATH);
        if (TextUtils.isEmpty(raw)) {
            return FALLBACK_TCP_ALGOS;
        }
        List<String> list = new ArrayList<>();
        for (String token : raw.trim().split("\\s+")) {
            if (!token.isEmpty() && !list.contains(token)) {
                list.add(token);
            }
        }
        return list.isEmpty() ? FALLBACK_TCP_ALGOS : list.toArray(new String[0]);
    }

    public static String getCurrentTcpAlgorithm() {
        String raw = FileUtils.readOneLine(TCP_CURRENT_PATH);
        return raw != null ? raw.trim() : null;
    }

    public static void setIoScheduler(String scheduler) {
        if (!TextUtils.isEmpty(scheduler)) {
            SystemProperties.set("sys.parts.io.scheduler", scheduler);
        }
    }

    public static void setReadAheadKb(String kb) {
        if (!TextUtils.isEmpty(kb)) {
            SystemProperties.set("sys.parts.io.read_ahead_kb", kb);
        }
    }

    public static void setTcpCongestion(String algo) {
        if (!TextUtils.isEmpty(algo)) {
            SystemProperties.set("sys.parts.net.tcp_congestion", algo);
        }
    }
}
