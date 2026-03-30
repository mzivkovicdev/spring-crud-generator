/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.springcrudgenerator.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class HashUtils {

    private HashUtils() {}

    /**
     * Calculate the SHA-256 hash of a given string.
     * The string is converted to a byte array using UTF-8 encoding before being hashed.
     * 
     * @param text String to hash
     * @return SHA-256 hash of the given string
     */
    public static String sha256(final String text) {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Calculate the SHA-256 hash of a given string.
     *
     * @param data Byte array to hash
     * @return SHA-256 hash of the given string
     */
    public static String sha256(final byte[] data) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] dig = md.digest(data);
            final StringBuilder sb = new StringBuilder(dig.length * 2);
            for (final byte b : dig) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
