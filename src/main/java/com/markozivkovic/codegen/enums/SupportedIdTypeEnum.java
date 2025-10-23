package com.markozivkovic.codegen.enums;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import com.markozivkovic.codegen.utils.StringUtils;

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
