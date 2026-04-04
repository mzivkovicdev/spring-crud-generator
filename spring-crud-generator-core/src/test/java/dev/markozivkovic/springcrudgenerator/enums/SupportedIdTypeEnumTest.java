package dev.markozivkovic.springcrudgenerator.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SupportedIdTypeEnumTest {

    @Test
    void getKey_shouldReturnCorrectKeysForAllEnumConstants() {
        assertEquals(Long.class.getSimpleName(), SupportedIdTypeEnum.LONG.getKey());
        assertEquals(Integer.class.getSimpleName(), SupportedIdTypeEnum.INTEGER.getKey());
        assertEquals(Short.class.getSimpleName(), SupportedIdTypeEnum.SHORT.getKey());
        assertEquals(Byte.class.getSimpleName(), SupportedIdTypeEnum.BYTE.getKey());
        assertEquals(String.class.getSimpleName(), SupportedIdTypeEnum.STRING.getKey());
        assertEquals(UUID.class.getSimpleName(), SupportedIdTypeEnum.UUID.getKey());
        assertEquals(BigInteger.class.getSimpleName(), SupportedIdTypeEnum.BIG_INTEGER.getKey());
        assertEquals(BigDecimal.class.getSimpleName(), SupportedIdTypeEnum.BIG_DECIMAL.getKey());
        assertEquals(Byte[].class.getSimpleName(), SupportedIdTypeEnum.BYTE_ARRAY.getKey());
    }

    @Test
    void resolveIdType_shouldReturnCorrectEnumForExactKey() {
        for (SupportedIdTypeEnum type : SupportedIdTypeEnum.values()) {
            SupportedIdTypeEnum resolved = SupportedIdTypeEnum.resolveIdType(type.getKey());
            assertEquals(type, resolved, "Expected to resolve " + type.getKey() + " to " + type);
        }
    }

    @Test
    void resolveIdType_shouldBeCaseInsensitive() {
        assertEquals(SupportedIdTypeEnum.LONG, SupportedIdTypeEnum.resolveIdType("long"));
        assertEquals(SupportedIdTypeEnum.INTEGER, SupportedIdTypeEnum.resolveIdType("InTeGeR"));
        assertEquals(SupportedIdTypeEnum.STRING, SupportedIdTypeEnum.resolveIdType("string"));
        assertEquals(SupportedIdTypeEnum.UUID, SupportedIdTypeEnum.resolveIdType("uuid"));
    }

    @Test
    void resolveIdType_shouldTrimInput() {
        assertEquals(SupportedIdTypeEnum.LONG, SupportedIdTypeEnum.resolveIdType("  Long  "));
        assertEquals(SupportedIdTypeEnum.BYTE_ARRAY, SupportedIdTypeEnum.resolveIdType("  " + Byte[].class.getSimpleName() + " "));
    }

    @Test
    void resolveIdType_shouldThrowForNull() {
        
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SupportedIdTypeEnum.resolveIdType(null)
        );
        assertEquals("ID type key cannot be null or empty", ex.getMessage());
    }

    @Test
    void resolveIdType_shouldThrowForEmptyString() {
        
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SupportedIdTypeEnum.resolveIdType("")
        );
        assertEquals("ID type key cannot be null or empty", ex.getMessage());
    }

    @Test
    void resolveIdType_shouldThrowForBlankString() {
        
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SupportedIdTypeEnum.resolveIdType("   ")
        );
        assertEquals("ID type key cannot be null or empty", ex.getMessage());
    }

    @Test
    void resolveIdType_shouldThrowForUnsupportedType() {
        
        final String key = "SomeUnknownType";

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SupportedIdTypeEnum.resolveIdType(key)
        );

        assertTrue(ex.getMessage().contains("Unsupported ID type"));
        assertTrue(ex.getMessage().contains(key));
        Arrays.stream(SupportedIdTypeEnum.values())
                .forEach(v -> assertTrue(ex.getMessage().contains(v.name())));
    }
    
}
