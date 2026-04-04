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

import java.util.Objects;

import dev.markozivkovic.springcrudgenerator.models.AuditDefinition.AuditTypeEnum;

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
