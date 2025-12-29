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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum BasicType {
    
    STRING(String.class.getSimpleName()),
    CHARACTER(Character.class.getSimpleName()),
    INTEGER(Integer.class.getSimpleName()),
    LONG(Long.class.getSimpleName()),
    BOOLEAN(Boolean.class.getSimpleName()),
    DOUBLE(Double.class.getSimpleName()),
    FLOAT(Float.class.getSimpleName()),
    SHORT(Short.class.getSimpleName()),
    BYTE(Byte.class.getSimpleName()),
    UUID(UUID.class.getSimpleName()),
    BIG_DECIMAL(BigDecimal.class.getSimpleName()),
    BIG_INTEGER(BigInteger.class.getSimpleName()),
    LOCAL_DATE(LocalDate.class.getSimpleName()),
    LOCAL_DATE_TIME(LocalDateTime.class.getSimpleName()),
    OFFSET_DATE_TIME(OffsetDateTime.class.getSimpleName()),
    INSTANT(Instant.class.getSimpleName());

    private final String key;
    private static final Set<String> KEYS_UPPER = Stream.of(values())
            .map(v -> v.key.toUpperCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    BasicType(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * Returns true if the given type is a basic type, false otherwise.
     * 
     * @param type the type to check
     * @return true if the type is a basic type, false otherwise
     */
    public static boolean isBasicType(final String type) {
        return Objects.nonNull(type) && KEYS_UPPER.contains(type.toUpperCase(Locale.ROOT));
    }

    /**
     * Returns a string containing all supported basic types, separated by commas.
     * 
     * @return a string containing all supported basic types, separated by commas
     */
    public static String getSupportedValues() {

        return Stream.of(values())
                .map(BasicType::getKey)
                .collect(Collectors.joining(", "));
    }

}
