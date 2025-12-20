package com.markozivkovic.codegen.utils;

import java.util.Objects;

import com.markozivkovic.codegen.models.AuditDefinition.AuditTypeEnum;

public class AuditUtils {
    
    private AuditUtils() {}

    /**
     * Resolve the given audit type enum to its corresponding Java type.
     *
     * @param auditTypeEnum The audit type enum to resolve.
     * @return The corresponding Java type as a string.
     * @throws IllegalArgumentException If the given audit type is unknown.
     */
    public static String resolveAuditType(final AuditTypeEnum auditTypeEnum) {

        if (Objects.isNull(auditTypeEnum)) {
            throw new IllegalArgumentException("Audit type cannot be null");
        }

        switch (auditTypeEnum) {
            case INSTANT:
                return "Instant";
            case LOCALDATE:
                return "LocalDate";
            case LOCALDATETIME:
                return "LocalDateTime";
            default:
                throw new IllegalArgumentException(
                    String.format("Unknown audit type: %s", auditTypeEnum)
                );
        }
    }

    /**
     * Resolve the given audit type enum to its corresponding Java import statement.
     *
     * @param auditTypeEnum The audit type enum to resolve.
     * @return The corresponding Java import statement as a string.
     * @throws IllegalArgumentException If the given audit type is unknown.
     */
    public static String resolveAuditingImport(final AuditTypeEnum auditTypeEnum) {
        return "java.time." + resolveAuditType(auditTypeEnum);
    }

}
