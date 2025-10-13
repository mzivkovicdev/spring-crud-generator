package com.markozivkovic.codegen.utils;

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

        switch (auditTypeEnum) {
            case INSTANT:
                return "Instant";
            case LOCAL_DATE:
                return "LocalDate";
            case LOCAL_DATE_TIME:
                return "LocalDateTime";
            default:
                throw new IllegalArgumentException(
                    String.format("Unknown audit type: %s", auditTypeEnum)
                );
        }
    }

    public static String resolveAuditingImport(final AuditTypeEnum auditTypeEnum) {
        return "java.time." + resolveAuditType(auditTypeEnum);
    }

}
