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

package dev.markozivkovic.springcrudgenerator.builders;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.flyway.SchemaDiff.Result;

/**
 * Builds the template context used to render an SQL alter-table migration.
 */
public final class AlterTableContextBuilder {

    private AlterTableContextBuilder() {
    }

    /**
     * Builds an alter-table template context from the schema difference and the corresponding
     * create-table context.
     *
     * @param tableName          the database table to alter; must not be {@code null}
     * @param database           the target database dialect; must not be {@code null}
     * @param diff               the detected schema changes to render; must not be {@code null}
     * @param createTableContext the create-table context supplying audit-column expressions and
     *                           types; must not be {@code null}
     * @return a mutable context containing the values required by the alter-table template
     */
    public static Map<String, Object> build(
            final String tableName,
            final DatabaseType database,
            final Result diff,
            final Map<String, Object> createTableContext) {

        final Map<String, Object> alterTableContext = new LinkedHashMap<>();
        alterTableContext.put("table", tableName);
        alterTableContext.put("addedColumns", diff.getAddedColumns());
        alterTableContext.put("removedColumns", diff.getRemovedColumns());
        alterTableContext.put("modifiedColumns", diff.getModifiedColumns());
        alterTableContext.put("pkChanged", diff.getPkChanged());
        alterTableContext.put("newPk", diff.getNewPk());
        alterTableContext.put("addedFks", diff.getAddedFks());
        alterTableContext.put("removedFks", diff.getRemovedFks());
        alterTableContext.put("db", database);
        alterTableContext.put("auditAdded", diff.isAuditAdded());
        alterTableContext.put("auditRemoved", diff.isAuditRemoved());
        alterTableContext.put("auditTypeChanged", diff.isAuditTypeChanged());
        alterTableContext.put("auditCreatedType", createTableContext.get("auditCreatedType"));
        alterTableContext.put("auditUpdatedType", createTableContext.get("auditUpdatedType"));
        alterTableContext.put("auditNowExpr", createTableContext.get("auditNowExpr"));
        alterTableContext.put(TemplateContextConstants.SOFT_DELETE_CHANGED, diff.getSoftDeleteChanged());
        alterTableContext.put(TemplateContextConstants.SOFT_DELETE_ENABLED, diff.getSoftDeleteEnabled());
        return alterTableContext;
    }
}
