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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.springcrudgenerator.models.flyway.SchemaDiff.AddedColumn;
import dev.markozivkovic.springcrudgenerator.models.flyway.SchemaDiff.FkChange;
import dev.markozivkovic.springcrudgenerator.models.flyway.SchemaDiff.Result;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;

class AlterTableContextBuilderTest {

    private static final String ALTER_TABLE_TEMPLATE = "migration/flyway/alter-table-combined.sql.ftl";
    private static final String TABLE_NAME = "orders";

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void build_whenSoftDeleteStateIsUnchanged_doesNotRenderSoftDeleteDdl(final boolean softDeleteEnabled) {
        final Result diff = new Result()
                .setAddedColumns(List.of(new AddedColumn("description", "VARCHAR(255)", true, false, null)))
                .setSoftDeleteChanged(false)
                .setSoftDeleteEnabled(softDeleteEnabled);

        final String sql = render(diff, DatabaseType.POSTGRESQL, softDeleteEnabled);

        assertTrue(sql.contains("description"));
        assertFalse(sql.contains("deleted"));
    }

    @Test
    void build_whenSoftDeleteIsEnabled_rendersColumnAndIndexCreation() {
        final Result diff = new Result()
                .setSoftDeleteChanged(true)
                .setSoftDeleteEnabled(true);

        final String sql = render(diff, DatabaseType.POSTGRESQL, true);

        assertTrue(sql.contains("ADD COLUMN \"deleted\""));
        assertTrue(sql.contains("CREATE INDEX ix_orders_deleted"));
    }

    @Test
    void build_whenSoftDeleteIsDisabled_rendersColumnAndIndexRemoval() {
        final Result diff = new Result()
                .setSoftDeleteChanged(true)
                .setSoftDeleteEnabled(false);

        final String sql = render(diff, DatabaseType.POSTGRESQL, false);

        assertTrue(sql.contains("DROP INDEX IF EXISTS ix_orders_deleted"));
        assertTrue(sql.contains("DROP COLUMN IF EXISTS \"deleted\""));
    }

    @Test
    void build_whenRelationColumnIsRemoved_dropsForeignKeyBeforeColumn() {
        final Result diff = new Result()
                .setRemovedColumns(List.of("customer_id"))
                .setRemovedFks(List.of(new FkChange("customer_id", "customers", "id")));

        final String sql = render(diff, DatabaseType.MYSQL, false);
        final int foreignKeyRemovalIndex = sql.indexOf("DROP FOREIGN KEY fk_orders_customer_id");
        final int columnRemovalIndex = sql.indexOf("DROP COLUMN `customer_id`");

        assertTrue(foreignKeyRemovalIndex >= 0);
        assertTrue(columnRemovalIndex > foreignKeyRemovalIndex);
    }

    private static String render(
            final Result diff,
            final DatabaseType database,
            final boolean softDeleteEnabled) {

        final Map<String, Object> createTableContext = Map.of(
                TemplateContextConstants.SOFT_DELETE_ENABLED,
                softDeleteEnabled);
        final Map<String, Object> alterTableContext = AlterTableContextBuilder.build(
                TABLE_NAME,
                database,
                diff,
                createTableContext);
        return FreeMarkerTemplateProcessorUtils.processTemplate(
                ALTER_TABLE_TEMPLATE,
                alterTableContext);
    }
}
