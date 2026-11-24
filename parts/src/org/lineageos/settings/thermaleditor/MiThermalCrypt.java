/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * Encryption format compatible with adithya2306/mi-thermal-crypt
 * (AES-128-CBC, key & IV = "thermalopenssl.h").
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

package org.lineageos.settings.thermaleditor;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class MiThermalCrypt {

    private static final String VENDOR_THERMAL_DIR = "/vendor/etc";
    private static final String STAGING_SUBDIR = "thermal_staging";
    private static final String CIPHER_TRANSFORM = "AES/CBC/PKCS5Padding";
    private static final byte[] KEY_BYTES = "thermalopenssl.h".getBytes(StandardCharsets.UTF_8);

    private MiThermalCrypt() {}

    public static final class DecryptedConfig {
        public final String content;
        public final boolean wasEncrypted;
        public final boolean isModified;

        public DecryptedConfig(String content, boolean wasEncrypted, boolean isModified) {
            this.content = content;
            this.wasEncrypted = wasEncrypted;
            this.isModified = isModified;
        }
    }

    public static File getStagingDir(Context context) {
        File dir = new File(context.getFilesDir(), STAGING_SUBDIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static boolean isValidFileName(String name) {
        return !TextUtils.isEmpty(name)
                && name.startsWith("thermal")
                && name.endsWith(".conf")
                && !name.contains("/")
                && !name.contains("..");
    }

    public static List<String> listVendorThermalConfigs() {
        File dir = new File(VENDOR_THERMAL_DIR);
        File[] files = dir.listFiles((d, name) -> isValidFileName(name));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>(files.length);
        for (File f : files) {
            names.add(f.getName());
        }
        Collections.sort(names);
        return names;
    }

    public static boolean isModified(Context context, String fileName) {
        if (!isValidFileName(fileName)) {
            return false;
        }
        return new File(getStagingDir(context), fileName).exists();
    }

    public static DecryptedConfig loadConfig(Context context, String fileName)
            throws IOException, GeneralSecurityException {
        if (!isValidFileName(fileName)) {
            throw new IOException("Invalid thermal config filename: " + fileName);
        }
        File staged = new File(getStagingDir(context), fileName);
        boolean modified = staged.exists();
        File source = modified ? staged : new File(VENDOR_THERMAL_DIR, fileName);

        byte[] raw = readAllBytes(source);
        if (looksLikePlainText(raw)) {
            return new DecryptedConfig(new String(raw, StandardCharsets.UTF_8), false, modified);
        }
        byte[] decrypted = decrypt(raw);
        return new DecryptedConfig(new String(decrypted, StandardCharsets.UTF_8), true, modified);
    }

    public static void saveAndApply(Context context, String fileName, String plainText,
            boolean encryptOutput) throws IOException, GeneralSecurityException {
        if (!isValidFileName(fileName)) {
            throw new IOException("Invalid thermal config filename: " + fileName);
        }
        byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] outBytes = encryptOutput ? encrypt(plainBytes) : plainBytes;

        File staged = new File(getStagingDir(context), fileName);
        writeAllBytes(staged, outBytes);

        SystemProperties.set("sys.parts.thermal.file", fileName);
        SystemProperties.set("sys.parts.thermal.action", "apply");
    }

    public static void resetConfig(Context context, String fileName) {
        if (!isValidFileName(fileName)) {
            return;
        }
        File staged = new File(getStagingDir(context), fileName);
        if (staged.exists()) {
            staged.delete();
        }
        SystemProperties.set("sys.parts.thermal.file", fileName);
        SystemProperties.set("sys.parts.thermal.action", "reset");
    }

    public static int resetAllConfigs(Context context) {
        File dir = getStagingDir(context);
        File[] files = dir.listFiles((d, name) -> isValidFileName(name));
        if (files == null || files.length == 0) {
            return 0;
        }
        int count = 0;
        for (File f : files) {
            String name = f.getName();
            if (f.delete()) {
                count++;
            }
            SystemProperties.set("sys.parts.thermal.file", name);
            SystemProperties.set("sys.parts.thermal.action", "reset");
        }
        return count;
    }

    public static byte[] encrypt(byte[] plainBytes) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORM);
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(KEY_BYTES, "AES"),
                new IvParameterSpec(KEY_BYTES));
        return cipher.doFinal(plainBytes);
    }

    public static byte[] decrypt(byte[] cipherBytes) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORM);
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(KEY_BYTES, "AES"),
                new IvParameterSpec(KEY_BYTES));
        return cipher.doFinal(cipherBytes);
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

    private static byte[] readAllBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int offset = 0;
            while (offset < buf.length) {
                int read = fis.read(buf, offset, buf.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            return offset == buf.length ? buf : Arrays.copyOf(buf, offset);
        }
    }

    private static void writeAllBytes(File file, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            fos.getFD().sync();
        }
    }
}
