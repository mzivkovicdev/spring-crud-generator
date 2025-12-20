package com.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.markozivkovic.codegen.constants.ImportConstants.Java;
import com.markozivkovic.codegen.models.AuditDefinition.AuditTypeEnum;

class AuditUtilsTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "INSTANT, Instant",
        "LOCALDATE, LocalDate",
        "LOCALDATETIME, LocalDateTime"
    })
    @DisplayName("resolveAuditType should return expected type")
    void resolveAuditType_shouldReturnExpectedType(final AuditTypeEnum auditTypeEnum, final String expected) {
        final String result = AuditUtils.resolveAuditType(auditTypeEnum);
        assertEquals(expected, result);
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

        final String result = AuditUtils.resolveAuditingImport(AuditTypeEnum.LOCALDATE);

        assertEquals(Java.LOCAL_DATE, result);
    }

    @Test
    @DisplayName("resolveAuditingImport should return full java.time import for LOCAL_DATE_TIME")
    void resolveAuditingImport_shouldReturnFullImport_forLocalDateTimeEnum() {

        final String result = AuditUtils.resolveAuditingImport(AuditTypeEnum.LOCALDATETIME);

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
