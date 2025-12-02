package com.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.markozivkovic.codegen.constants.ImportConstants.Java;
import com.markozivkovic.codegen.models.AuditDefinition.AuditTypeEnum;

class AuditUtilsTest {

    @Test
    @DisplayName("resolveAuditType should return Instant for INSTANT")
    void resolveAuditType_shouldReturnInstant_forInstantEnum() {

        final String result = AuditUtils.resolveAuditType(AuditTypeEnum.INSTANT);

        assertEquals("Instant", result);
    }

    @Test
    @DisplayName("resolveAuditType should return LocalDate for LOCAL_DATE")
    void resolveAuditType_shouldReturnLocalDate_forLocalDateEnum() {

        final String result = AuditUtils.resolveAuditType(AuditTypeEnum.LOCAL_DATE);

        assertEquals("LocalDate", result);
    }

    @Test
    @DisplayName("resolveAuditType should return LocalDateTime for LOCAL_DATE_TIME")
    void resolveAuditType_shouldReturnLocalDateTime_forLocalDateTimeEnum() {

        final String result = AuditUtils.resolveAuditType(AuditTypeEnum.LOCAL_DATE_TIME);

        assertEquals("LocalDateTime", result);
    }

    @Test
    @DisplayName("resolveAuditingImport should return full java.time import for INSTANT")
    void resolveAuditingImport_shouldReturnFullImport_forInstantEnum() {

        final String result = AuditUtils.resolveAuditingImport(AuditTypeEnum.INSTANT);

        assertEquals(Java.INSTANT, result);
    }

    @Test
    @DisplayName("resolveAuditingImport should return full java.time import for LOCAL_DATE")
    void resolveAuditingImport_shouldReturnFullImport_forLocalDateEnum() {

        final String result = AuditUtils.resolveAuditingImport(AuditTypeEnum.LOCAL_DATE);

        assertEquals(Java.LOCAL_DATE, result);
    }

    @Test
    @DisplayName("resolveAuditingImport should return full java.time import for LOCAL_DATE_TIME")
    void resolveAuditingImport_shouldReturnFullImport_forLocalDateTimeEnum() {

        final String result = AuditUtils.resolveAuditingImport(AuditTypeEnum.LOCAL_DATE_TIME);

        assertEquals(Java.LOCAL_DATE_TIME, result);
    }

    @Test
    @DisplayName("resolveAuditType should throw IllegalArgumentException when auditTypeEnum is null")
    void resolveAuditType_shouldThrow_whenAuditTypeEnumIsNull() {

        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> AuditUtils.resolveAuditType(null)
        );

        assertTrue(ex.getMessage().contains("Audit type cannot be null"));
    }

}
