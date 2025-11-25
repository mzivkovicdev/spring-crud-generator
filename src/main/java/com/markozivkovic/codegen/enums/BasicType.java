package com.markozivkovic.codegen.enums;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    LOCAL_DATE_TIME(LocalDateTime.class.getSimpleName());

    private final String key;
    private static final Set<String> KEYS_UPPER = Stream.of(values())
            .map(v -> v.key.toUpperCase())
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
        return Objects.nonNull(type) && KEYS_UPPER.contains(type.toUpperCase());
    }

}
