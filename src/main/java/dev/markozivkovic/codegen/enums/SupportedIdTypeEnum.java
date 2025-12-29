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

package dev.markozivkovic.codegen.enums;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import dev.markozivkovic.codegen.utils.StringUtils;

public enum SupportedIdTypeEnum {
    
    LONG(Long.class.getSimpleName()),
    INTEGER(Integer.class.getSimpleName()),
    SHORT(Short.class.getSimpleName()),
    BYTE(Byte.class.getSimpleName()),
    STRING(String.class.getSimpleName()),
    UUID(UUID.class.getSimpleName()),
    BIG_INTEGER(BigInteger.class.getSimpleName()),
    BIG_DECIMAL(BigDecimal.class.getSimpleName()),
    BYTE_ARRAY(Byte[].class.getSimpleName());

    private String key;

    SupportedIdTypeEnum(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * Resolves the given key to a supported ID type. If the key does not match any supported ID type,
     * an {@link IllegalArgumentException} is thrown.
     *
     * @param key the key of the ID type to resolve
     * @return the resolved ID type
     * @throws IllegalArgumentException if the key does not match any supported ID type
     */
    public static SupportedIdTypeEnum resolveIdType(final String key) {

        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("ID type key cannot be null or empty");
        }
        final String trimmedKey = key.trim();

        return Stream.of(values())
                .filter(type -> type.getKey().equalsIgnoreCase(trimmedKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format(
                        "Unsupported ID type: %s. Supported ID types are: %s", key, Arrays.asList(values())
                    )
                ));
    }
}
