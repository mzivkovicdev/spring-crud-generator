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

package dev.markozivkovic.codegen.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.markozivkovic.codegen.enums.SpecialType;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.RelationDefinition;
import dev.markozivkovic.codegen.models.CrudConfiguration.DatabaseType;
import dev.markozivkovic.codegen.models.IdDefinition.IdStrategyEnum;
import dev.markozivkovic.codegen.models.flyway.MigrationState;

public class FlywayUtils {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayUtils.class);

    private static final String CRUD_GENERATOR_DIR = ".crud-generator";
    private static final String MIGRATION_STATE_FILE = "migration-state.json";
    private static final int DEFAULT_VARCHAR = 255;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private FlywayUtils() {

    }

    /**
     * Return the VARCHAR or TEXT type to use based on the given {@link DatabaseType} and length.
     * <p>
     * The returned type is:
     * <ul>
     *     <li>For POSTGRESQL, VARCHAR({@code len})</li>
     *     <li>For MYSQL, VARCHAR({@code len})</li>
     *     <li>For MSSQL, NVARCHAR({@code len})</li>
     * </ul>
     * @param d the {@link DatabaseType}
     * @param len the length of the VARCHAR or TEXT type
     * @return the VARCHAR or TEXT type to use
     */
    private static String varcharOrText(DatabaseType d, int len) {
        return switch (d) {
            case POSTGRESQL -> "VARCHAR(" + len + ")";
            case MYSQL -> "VARCHAR(" + len + ")";
            case MSSQL -> "NVARCHAR(" + len + ")";
        };
    }

    /**
     * Returns the type to use for a {@link java.time.LocalDateTime} in the given
     * {@link DatabaseType}.
     * <p>
     * The returned type is:
     * <ul>
     *     <li>For POSTGRESQL, TIMESTAMP</li>
     *     <li>For MYSQL, DATETIME(6)</li>
     *     <li>For MSSQL, DATETIME2(6)</li>
     * </ul>
     * @param d the {@link DatabaseType}
     * @return the type to use for a {@link java.time.LocalDateTime}
     */
    private static String localDateTimeType(DatabaseType d) {
        return switch (d) {
            case POSTGRESQL -> "TIMESTAMP";
            case MYSQL -> "DATETIME(6)";
            case MSSQL -> "DATETIME2(6)";
        };
    }
    
    /**
     * Returns the type to use for a {@link java.time.Instant} in the given
     * {@link DatabaseType}.
     * <p>
     * The returned type is:
     * <ul>
     *     <li>For POSTGRESQL, TIMESTAMP WITH TIME ZONE</li>
     *     <li>For MYSQL, DATETIME(6)</li>
     *     <li>For MSSQL, DATETIME2(6)</li>
     * </ul>
     * @param d the {@link DatabaseType}
     * @return the type to use for a {@link java.time.Instant}
     */
    private static String instantType(DatabaseType d) {
        return switch (d) {
            case POSTGRESQL -> "TIMESTAMP WITH TIME ZONE";
            case MYSQL -> "DATETIME(6)";
            case MSSQL -> "DATETIME2(6)";
        };
    }

    /**
     * Returns the default type for a DECIMAL column in the given
     * {@link DatabaseType}.
     * <p>
     * The returned type is:
     * <ul>
     *     <li>For POSTGRESQL, NUMERIC(19,2)</li>
     *     <li>For MYSQL, DECIMAL(19,2)</li>
     *     <li>For MSSQL, DECIMAL(19,2)</li>
     * </ul>
     * @param d the {@link DatabaseType}
     * @return the default type for a DECIMAL column
     */
    private static String decimalDefault(DatabaseType d) {
        return switch (d) {
            case POSTGRESQL -> "NUMERIC(19,2)";
            case MYSQL -> "DECIMAL(19,2)";
            case MSSQL -> "DECIMAL(19,2)";
        };
    }

    /**
     * Returns the default type for a BIGINTEGER column in the given
     * {@link DatabaseType}.
     * <p>
     * The returned type is:
     * <ul>
     *     <li>For POSTGRESQL, NUMERIC</li>
     *     <li>For MYSQL, DECIMAL(65,0)</li>
     *     <li>For MSSQL, DECIMAL(38,0)</li>
     * </ul>
     * @param d the {@link DatabaseType}
     * @return the default type for a BIGINTEGER column
     */
    private static String bigIntegerDefault(final DatabaseType d) {
        return switch (d) {
            case POSTGRESQL -> "NUMERIC";
            case MYSQL -> "DECIMAL(65,0)";
            case MSSQL -> "DECIMAL(38,0)";
        };
    }

    /**
     * Returns the type to use for a JSON column in the given
     * {@link DatabaseType}.
     * <p>
     * The returned type is:
     * <ul>
     *     <li>For POSTGRESQL, JSONB</li>
     *     <li>For MYSQL, JSON</li>
     *     <li>For MSSQL, NVARCHAR(MAX)</li>
     * </ul>
     * @param d the {@link DatabaseType}
     * @return the type to use for a JSON column
     */
    private static String jsonType(DatabaseType d) {
        return switch (d) {
            case POSTGRESQL -> "JSONB";
            case MYSQL -> "JSON";
            case MSSQL -> "NVARCHAR(MAX)";
        };
    }

    /**
     * Returns the type to use for a UUID column in the given
     * {@link DatabaseType}.
     * <p>
     * The returned type is:
     * <ul>
     *     <li>For POSTGRESQL, UUID</li>
     *     <li>For MYSQL, BINARY(16)</li>
     *     <li>For MSSQL, UNIQUEIDENTIFIER</li>
     * </ul>
     * @param d the {@link DatabaseType}
     * @return the type to use for a UUID column
     */
    private static String uuidType(DatabaseType d) {
        return switch (d) {
            case POSTGRESQL -> "UUID";
            case MYSQL -> "BINARY(16)";
            case MSSQL -> "UNIQUEIDENTIFIER";
        };
    }

    /**
     * Maps a Java audit type to the corresponding SQL type for a given
     * {@link DatabaseType}.
     * <p>
     * The returned type is:
     * <ul>
     *     <li>For LocalDate, DATE</li>
     *     <li>For LocalDateTime, TIMESTAMP, TIMESTAMP WITH TIME ZONE or
     *         DATETIME2(6)</li>
     *     <li>For OffsetDateTime, TIMESTAMP WITH TIME ZONE, DATETIME(6) or
     *         DATETIMEOFFSET(6)</li>
     *     <li>For Instant, TIMESTAMP WITH TIME ZONE, DATETIME(6) or
     *         DATETIME2(6)</li>
     * </ul>
     * @param d the {@link DatabaseType}
     * @param auditType the Java audit type
     * @return the SQL type
     */
    private static String auditType(final DatabaseType d, final String auditType) {
        if ("LocalDate".equals(auditType)) return "DATE";
        if ("LocalDateTime".equals(auditType)) return localDateTimeType(d);
        if ("OffsetDateTime".equals(auditType)) {
            return switch (d) {
                case POSTGRESQL -> "TIMESTAMP WITH TIME ZONE";
                case MYSQL -> "DATETIME(6)";
                case MSSQL -> "DATETIMEOFFSET(6)";
            };
        }
        return switch (d) {
            case POSTGRESQL -> "TIMESTAMP WITH TIME ZONE";
            case MYSQL -> "DATETIME(6)";
            case MSSQL -> "DATETIME2(6)";
        };
    }

    /**
     * Maps a database type to a SQL expression that evaluates to the current
     * timestamp. This is used for the default values of the audit columns.
     *
     * @param d The database type to map.
     * @return The SQL expression that evaluates to the current timestamp.
     */
    private static String auditNow(DatabaseType d) {
        return switch (d) {
            case POSTGRESQL -> "now()";
            case MYSQL -> "CURRENT_TIMESTAMP(6)";
            case MSSQL -> "SYSUTCDATETIME()";
        };
    }

    /**
     * Maps a Java type to a SQL type.
     *
     * @param fieldDefinition the field definition for which the SQL type should be determined
     * @return the SQL type as a string
     */
    public static String columnSqlType(final FieldDefinition fieldDefinition, final DatabaseType databaseType) {
        final String t = fieldDefinition.getType();
        
        if (t == null) {
            return varcharOrText(databaseType, DEFAULT_VARCHAR);
        }
        
        switch (t) {
            case "Long": return "BIGINT";
            case "Integer": return databaseType == DatabaseType.MSSQL ? "INT" : "INTEGER";
            case "UUID": return uuidType(databaseType);
            case "LocalDate": return "DATE";
            case "LocalDateTime": return localDateTimeType(databaseType);
            case "Instant": return instantType(databaseType);
            case "Enum": return varcharOrText(databaseType, DEFAULT_VARCHAR);
            case "String": {
                final Integer length = (fieldDefinition.getColumn() != null && fieldDefinition.getColumn().getLength() != null) ?
                        fieldDefinition.getColumn().getLength() : DEFAULT_VARCHAR;
                return varcharOrText(databaseType, length);
            }
            case "Boolean": return databaseType == DatabaseType.MSSQL ? "BIT" : "BOOLEAN";
            case "Double": return databaseType == DatabaseType.MYSQL ? "DOUBLE" : "DOUBLE PRECISION";
            case "Float": return "REAL";
            case "BigDecimal": return decimalDefault(databaseType);
            case "BigInteger": return bigIntegerDefault(databaseType);
            case "Byte": return "SMALLINT";
            case "Short": return "SMALLINT";
            case "Char": return "CHAR(1)";
            case "Character": return "CHAR(1)";
            default:
                if (t.startsWith("JSON<")) return jsonType(databaseType);
                LOGGER.warn("Unknown field type '{}', falling back to BIGINT", t);
                return "BIGINT";
        }
    }

    /**
     * Returns a Flyway identity decorator for the given field definition and table.
     * If the field definition does not have an ID definition, or the ID definition
     * does not have a strategy, this method returns null.
     * For strategies IDENTITY and AUTO, this method returns a Flyway string to define
     * an auto-incrementing column for the given database type.
     * For strategy SEQUENCE, this method returns a Flyway string to define a column
     * that uses a sequence for its values.
     * For any other strategy, this method returns null.
     * @param databaseType the database type for which the Flyway decorator should be generated
     * @param fieldDefinition the field definition for which the Flyway decorator should be generated
     * @param table the table name for which the Flyway decorator should be generated
     * @return a Flyway string to define the identity column, or null if the field definition
     * does not have an ID definition or the ID definition does not have a strategy
     */
    public static String identityDecorateIfNeeded(final DatabaseType databaseType, final FieldDefinition fieldDefinition, final String table) {

        if (fieldDefinition.getId() == null || fieldDefinition.getId().getStrategy() == null) {
            return null;
        }

        switch (fieldDefinition.getId().getStrategy()) {
            case IDENTITY:
                return switch (databaseType) {
                    case POSTGRESQL -> "BIGINT GENERATED BY DEFAULT AS IDENTITY";
                    case MYSQL -> "BIGINT AUTO_INCREMENT";
                    case MSSQL -> "BIGINT IDENTITY(1,1)";
                };
            case AUTO:
                return switch (databaseType) {
                    case POSTGRESQL -> "BIGINT";
                    case MYSQL -> "BIGINT AUTO_INCREMENT";
                    case MSSQL -> "BIGINT IDENTITY(1,1)";
                };
            case SEQUENCE:
                return switch (databaseType) {
                    case POSTGRESQL, MSSQL, MYSQL -> "BIGINT";
                };
            case TABLE:
            default:
                return null;
        }
    }

    /**
     * If the given field definition has an ID definition with a strategy of SEQUENCE,
     * then this method returns a Flyway string to define a default value for the
     * column that is equal to the next value of the given sequence. If the field
     * definition does not have an ID definition or the ID definition does not have a
     * strategy, then this method returns null.
     * @param databaseType the database type for which the Flyway decorator should be generated
     * @param fieldDefinition the field definition for which the Flyway decorator should be generated
     * @param table the table name for which the Flyway decorator should be generated
     * @return a Flyway string to define the default value of the column based on a sequence,
     * or null if the field definition does not have an ID definition or the ID definition
     * does not have a strategy
     */
    private static String sequenceDefaultExpr(final DatabaseType d, final FieldDefinition f, final String table) {

        if (f.getId() == null) {
            return null;
        }

        final boolean needsSequenceDefault = IdStrategyEnum.SEQUENCE.equals(f.getId().getStrategy())
            || (IdStrategyEnum.AUTO.equals(f.getId().getStrategy()) && (DatabaseType.POSTGRESQL.equals(d) || DatabaseType.MSSQL.equals(d)));

        if (!needsSequenceDefault) {
            return null;
        }

        final String seq = (StringUtils.isNotBlank(f.getId().getSequenceName()))
                ? f.getId().getSequenceName()
                : table + "_id_seq";

        return switch (d) {
            case POSTGRESQL -> "nextval('" + seq + "')";
            case MSSQL      -> "NEXT VALUE FOR " + seq;
            case MYSQL      -> null;
        };
    }

    /**
     * Determines if the given field definition has a one-to-one or many-to-one relation.
     *
     * @param f the field definition to check
     * @return true if the field definition has a one-to-one or many-to-one relation, false otherwise
     */
    private static boolean isToOneRelation(final FieldDefinition f) {
        if (Objects.isNull(f.getRelation()))
            return false;
        final String t = f.getRelation().getType();
        return "OneToOne".equals(t) || "ManyToOne".equals(t);
    }

    /**
     * Determines if the given field definition has a collection relation, i.e., either
     * a one-to-many or many-to-many relation.
     *
     * @param f the field definition to check
     * @return true if the field definition has a one-to-many or many-to-many relation, false otherwise
     */
    private static boolean isCollectionRelation(final FieldDefinition f) {
        return f.getRelation() != null &&
            ("OneToMany".equals(f.getRelation().getType()) || "ManyToMany".equals(f.getRelation().getType()));
    }

    /**
     * Returns the column name for the given field definition, with the following rules:
     * <ul>
     * <li>If the field definition has a relation and the relation has an explicit join column name,
     * then that name is returned.</li>
     * <li>If the field definition has a relation and the relation does not have an explicit join column name,
     * then the snake-cased name of the field definition is returned, with "_id" appended to the end.</li>
     * <li>If the field definition does not have a relation, then the snake-cased name of the field definition is returned.</li>
     * </ul>
     *
     * @param f the field definition for which to resolve the column name
     * @param toOne whether to resolve the column name for a one-to-one relation
     * @return the resolved column name
     */
    private static String resolveColumnName(final FieldDefinition f, final boolean toOne) {
        if (toOne) {
            final String explicit = f.getRelation().getJoinColumn();
            return StringUtils.isNotBlank(explicit)
                    ? explicit
                    : ModelNameUtils.toSnakeCase(StringUtils.uncapitalize(f.getName())) + "_id";
        }
        return ModelNameUtils.toSnakeCase(StringUtils.uncapitalize(f.getName()));
    }
    
    /**
     * Resolves the SQL type of the primary key of the target model of the given relation field.
     * If the target model does not exist, or the target model does not have a primary key,
     * the given fallback value is returned.
     *
     * @param db the database type for which to resolve the SQL type
     * @param relField the relation field for which to resolve the target primary key's SQL type
     * @param modelsByName a map of model names to their definitions
     * @param fallback the value to return if the target model does not exist or does not have a primary key
     * @return the resolved SQL type of the target primary key, or the given fallback value
     */
    private static String sqlTypeOfTargetPk(final DatabaseType db,
                                            final FieldDefinition relField,
                                            final Map<String, ModelDefinition> modelsByName,
                                            final String fallback) {
        final ModelDefinition target = modelsByName.get(relField.getType());
        if (target == null) {
            return fallback;
        }

        final FieldDefinition targetPk = FieldUtils.extractIdField(target.getFields());
        if (targetPk == null) {
            return fallback;
        }

        return columnSqlType(targetPk, db);
    }

    /**
     * Resolves the foreign key constraints for the given model definition and adds them to a context map.
     * 
     * The returned context map will contain a single entry with key "fks" and value a list of foreign key
     * definitions. Each foreign key definition is a map with the following entries:
     * The returned context map will be empty if the given model definition does not have any one-to-one
     * or many-to-one relations.
     * 
     * @param model the model definition for which to resolve the foreign key constraints
     * @param modelsByName a map of model names to their definitions
     * @return a context map containing the foreign key constraints of the given model definition
     */
    public static Map<String, Object> toForeignKeysContext(
        final ModelDefinition model,
        final Map<String, ModelDefinition> modelsByName) {

        final String tableName = model.getStorageName();
        final List<Map<String, Object>> fks = new ArrayList<>();

        for (final FieldDefinition field : model.getFields()) {
            if (!isToOneRelation(field)) continue;

            final String columnName = resolveColumnName(field, true);
            final ModelDefinition target = modelsByName.get(field.getType());

            if (target == null) continue;

            final FieldDefinition targetPk = FieldUtils.extractIdField(target.getFields());
            final String refTable = (target.getStorageName() != null && !target.getStorageName().isBlank())
                    ? target.getStorageName()
                    : ModelNameUtils.toSnakeCase(StringUtils.uncapitalize(target.getName()));
            final String refPkCol = (targetPk != null)
                    ? ModelNameUtils.toSnakeCase(StringUtils.uncapitalize(targetPk.getName()))
                    : "id";

            final Map<String, Object> fk = new LinkedHashMap<>();
            fk.put("table", tableName);
            fk.put("name", "fk_" + tableName + "_" + columnName);
            fk.put("column", columnName);
            fk.put("refTable", refTable);
            fk.put("refColumn", refPkCol);
            fks.add(fk);
        }

        final Map<String, Object> ctx = new LinkedHashMap<>();
        if (!fks.isEmpty()) {
            ctx.put("fks", fks);
        }
        return ctx;
    }

    /**
     * Collects extra columns that should be added to the child tables of one-to-many
     * relations. The columns represent the foreign key to the parent table, and are
     * used when generating create table scripts.
     * 
     * @param allEntities the list of all entities
     * @param db          the database type
     * @param modelsByName the mapping of model names to ModelDefinition objects
     * @return a map of child table names to lists of extra columns
     */
    public static Map<String, List<Map<String,Object>>> collectReverseOneToManyExtras(
            final List<ModelDefinition> allEntities,
            final DatabaseType db,
            final Map<String, ModelDefinition> modelsByName
    ) {
        final Map<String, List<Map<String,Object>>> extras = new LinkedHashMap<>();

        for (final ModelDefinition parent : allEntities) {
            final FieldDefinition parentPk = FieldUtils.extractIdField(parent.getFields());
            final String parentPkSql = (parentPk != null) ? columnSqlType(parentPk, db) : "BIGINT";

            for (FieldDefinition f : parent.getFields()) {
                if (!isOneToMany(f)) continue;

                final ModelDefinition child = modelsByName.get(f.getType());
                if (child == null || child.getStorageName()==null) continue;

                final String childTable = child.getStorageName();

                final String childFkCol = (f.getRelation().getJoinColumn()!=null && !f.getRelation().getJoinColumn().isBlank())
                        ? f.getRelation().getJoinColumn()
                        : ModelNameUtils.toSnakeCase(parent.getName()) + "_id";

                final Map<String,Object> col = new LinkedHashMap<>();
                col.put("name", childFkCol);
                col.put("sqlType", parentPkSql);
                col.put("nullable", true);
                col.put("unique", false);
                col.put("isPk", false);

                extras.computeIfAbsent(childTable, k -> new ArrayList<>());
                final List<Map<String,Object>> dst = extras.get(childTable);
                final boolean already = dst.stream().anyMatch(m -> childFkCol.equals(m.get("name")));
                if (!already) dst.add(col);
            }
        }
        return extras;
    }

    /**
     * Checks if the given field definition has a relation of type {@link RelationDefinition#ONE_TO_MANY}.
     *
     * @param f the field definition to check
     * @return true if the field definition has a one-to-many relation, false otherwise
     */
    private static boolean isOneToMany(final FieldDefinition f) {
        return f.getRelation() != null && "OneToMany".equals(f.getRelation().getType());
    }
    
    /**
     * Maps a model definition to a Flyway migration context.
     *
     * @param model the model definition to be converted into a Flyway migration context
     * @param db the database type for which to generate the migration context
     * @param modelsByName a map of model names to their definitions
     * @param extraColumnsForThisTable a list of extra columns to be added to the migration context
     * @param optimisticLockingEnabled true if optimistic locking is enabled, false otherwise
     * @return a Flyway migration context as a map
     */
    public static Map<String, Object> toCreateTableContext(final ModelDefinition model, final DatabaseType db,
            final Map<String, ModelDefinition> modelsByName, final List<Map<String,Object>> extraColumnsForThisTable,
            final Boolean optimisticLockingEnabled) {

        final String tableName = model.getStorageName();

        final List<Map<String, Object>> cols   = new ArrayList<>();
        final List<Map<String, String>> checks = new ArrayList<>();
        String pkCols = null;

        for (final FieldDefinition field : model.getFields()) {

            if (isCollectionRelation(field) || SpecialType.isCollectionType(field.getType())) {
                continue;
            }

            final Map<String, Object> column = new LinkedHashMap<>();
            final boolean toOne = isToOneRelation(field);

            final String columnName = resolveColumnName(field, toOne);
            column.put("name", columnName);

            if (toOne) {
                column.put("sqlType", sqlTypeOfTargetPk(db, field, modelsByName, "BIGINT"));
            } else {
                column.put("sqlType", columnSqlType(field, db));

                if (field.getId() != null) {
                    final String identityType = identityDecorateIfNeeded(db, field, tableName);
                    if (identityType != null) {
                        column.put("sqlType", identityType);
                    }
                    final String seqDefault = sequenceDefaultExpr(db, field, tableName);
                    if (seqDefault != null) {
                        column.put("defaultExpr", seqDefault);
                    }
                }
            }

            final boolean nullable = field.getColumn() == null || Boolean.TRUE.equals(field.getColumn().getNullable());
            column.put("nullable", nullable);

            final boolean unique = field.getColumn() != null && Boolean.TRUE.equals(field.getColumn().getUnique());
            column.put("unique", unique);

            final boolean isPk = field.getId() != null;
            column.put("isPk", isPk);
            if (isPk) pkCols = columnName;

            if ("Enum".equals(field.getType()) && field.getValues() != null && !field.getValues().isEmpty()) {
                final Map<String, String> ck = new LinkedHashMap<>();
                ck.put("name", "ck_" + tableName + "_" + columnName + "_enum");
                ck.put("expr", columnName + " IN (" +
                        field.getValues().stream().map(v -> "'" + v + "'").collect(java.util.stream.Collectors.joining(", "))
                        + ")");
                checks.add(ck);
            }

            cols.add(column);
        }

        if (Boolean.TRUE.equals(optimisticLockingEnabled)) {
            cols.add(computeOptimisticLockingContext());
        }

        if (extraColumnsForThisTable != null && !extraColumnsForThisTable.isEmpty()) {
            final Set<String> existing = cols.stream()
                    .map(c -> (String) c.get("name"))
                    .collect(Collectors.toSet());
            for (final Map<String,Object> extra : extraColumnsForThisTable) {
                final String name = (String) extra.get("name");
                if (!existing.contains(name)){
                    cols.add(extra);
                    existing.add(name);
                }
            }
        }

        final boolean auditEnabled = model.getAudit() != null && model.getAudit().isEnabled();
        final String resolvedAuditType = auditEnabled
                ? AuditUtils.resolveAuditType(model.getAudit().getType())
                : "";

        final Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("tableName", tableName);
        ctx.put("columns", cols);
        if (!checks.isEmpty()) ctx.put("checks", checks);
        if (pkCols != null) ctx.put("pkColumns", pkCols);

        ctx.put("auditEnabled", auditEnabled);
        ctx.put("auditCreatedType", auditType(db, resolvedAuditType));
        ctx.put("auditUpdatedType", auditType(db, resolvedAuditType));
        ctx.put("auditNowExpr", auditNow(db));

        return ctx;
    }

    /**
     * Converts a model definition into a context map that can be used
     * to generate a flyway sequence generator SQL script.
     *
     * The context map contains the following keys:
     * <ul>
     *     <li>name - the name of the sequence generator</li>
     *     <li>initialValue - the initial value of the sequence generator (defaults to 1)</li>
     *     <li>allocatedSize - the allocation size of the sequence generator (defaults to 50)</li>
     *     <li>sequence - a boolean indicating that the context is for a sequence generator (always true)</li>
     * </ul>
     *
     * @param model the model definition to convert
     * @return the context map
     */
    public static Map<String, Object> toSequenceGeneratorContext(final ModelDefinition model) {

        final String seqName = generatorName(ModelNameUtils.stripSuffix(model.getName()));
        final FieldDefinition idField = FieldUtils.extractIdField(model.getFields());

        return Map.of(
                "name", seqName,
                "initialValue", idField.getId().getInitialValue() != null ? idField.getId().getInitialValue() : 1,
                "allocationSize", idField.getId().getAllocationSize() != null ? idField.getId().getAllocationSize() : 50,
                "sequence", true
        );
    }

    /**
     * Converts a model definition into a context map that can be used
     * to generate a flyway table generator SQL script.
     *
     * The context map contains the following keys:
     * <ul>
     *     <li>name - the name of the table generator</li>
     *     <li>pkColumnName - the primary key column name of the table generator (defaults to "gen_name")</li>
     *     <li>valueColumnName - the value column name of the table generator (defaults to "gen_value")</li>
     *     <li>table - a boolean indicating that the context is for a table generator (always true)</li>
     *     <li>initialValue - the initial value of the table generator (defaults to 1)</li>
     *     <li>pkColumnValue - the value of the primary key column of the table generator (defaults to the storage name of the model)</li>
     * </ul>
     *
     * @param model the model definition to convert
     * @return the context map
     */
    public static Map<String, Object> toTableGeneratorContext(final ModelDefinition model) {

        final String genName = generatorName(ModelNameUtils.stripSuffix(model.getName()));
        final FieldDefinition idField = FieldUtils.extractIdField(model.getFields());

        return Map.of(
                "name", genName,
                "pkColumnName", idField.getId().getPkColumnName() != null ? idField.getId().getPkColumnName() : "gen_name",
                "valueColumnName", idField.getId().getValueColumnName() != null ? idField.getId().getValueColumnName() : "gen_value",
                "table", true,
                "initialValue", idField.getId().getInitialValue() != null ? idField.getId().getInitialValue() : 1,
                "pkColumnValue", model.getStorageName()
        );
    }

    /**
     * Returns a Flyway sequence generator name based on the given model name.
     * The format of the sequence generator name is as follows: <strippedModelName>_gen.
     * 
     * @param strippedModelName the stripped model name
     * @return the Flyway sequence generator name
     */
    private static String generatorName(final String strippedModelName) {
        return String.format("%s_gen", strippedModelName);
    }

    /**
     * Computes the Flyway migration context map for the "version" field which is used for optimistic locking.
     * 
     * @return a map containing the name, sql type, nullable, unique and default expression for the "version" field
     */
    private static Map<String, Object> computeOptimisticLockingContext() {

        return Map.of(
                "name", "version",
                "sqlType", "BIGINT",
                "nullable", false,
                "unique", false,
                "isPk", false,
                "defaultExpr", "0"
        );
    }

    /**
     * Collects all element collection tables for the given model definition and database type.
     * 
     * @param model the model definition for which to collect the element collection tables
     * @param db the database type for which to collect the element collection tables
     * 
     * @return a list of maps containing the table name, owner table name, join column, owner PK column name, owner PK SQL type, value column name, value SQL type, whether the collection is a list, whether the collection needs a unique index, and the name of the order index column (if applicable)
     */
    public static List<Map<String, Object>> collectElementCollectionTables(final ModelDefinition model, final DatabaseType db) {

        final List<Map<String, Object>> result = new ArrayList<>();

        final String ownerTable = model.getStorageName();
        final String strippedModelName = ModelNameUtils.stripSuffix(model.getName());
        final String joinColumn = StringUtils.uncapitalize(strippedModelName) + "_id";
        final FieldDefinition ownerPk = FieldUtils.extractIdField(model.getFields());
        final String ownerPkColumn = ModelNameUtils.toSnakeCase(StringUtils.uncapitalize(ownerPk.getName()));
        final String ownerPkSqlType = columnSqlType(ownerPk, db);

        model.getFields().forEach(field -> {

            if (!SpecialType.isCollectionType(field.getType())) {
                return;
            }

            final boolean isList = SpecialType.isListType(field.getType());
            final boolean isSet = SpecialType.isSetType(field.getType());
            final String tableName = ownerTable + "_" + ModelNameUtils.toSnakeCase(StringUtils.uncapitalize(field.getName()));
            final String valueColumn = ModelNameUtils.toSnakeCase(StringUtils.uncapitalize(field.getName()));
            final String valueSqlType = columnSqlType(
                    new FieldDefinition().setColumn(field.getColumn())
                            .setName(field.getName())
                            .setType(FieldUtils.extractSimpleCollectionType(field)),
                    db
            );
            final Map<String, Object> ct = new LinkedHashMap<>();
            ct.put("tableName", tableName);
            ct.put("ownerTable", ownerTable);
            ct.put("joinColumn", joinColumn);
            ct.put("ownerPkColumn", ownerPkColumn);
            ct.put("ownerPkSqlType", ownerPkSqlType);
            ct.put("valueColumn", valueColumn);
            ct.put("valueSqlType", valueSqlType);
            ct.put("isList", isList);
            ct.put("needsUnique", isSet);
            ct.put("orderColumn", "order_index");
            result.add(ct);
        });
        
        return result;
    }

    /**
     * Maps a many-to-many relation field to a Flyway migration context map.
     *
     * @param owner the model definition that owns the relation field
     * @param relationField the relation field definition
     * @param db the database type for which to generate the migration context
     * @param modelsByName a map of model names to their definitions
     * @return a Flyway migration context map
     */
    public static Map<String, Object> toJoinTableContext(
            final ModelDefinition owner,
            final FieldDefinition relationField,
            final DatabaseType db,
            final Map<String, ModelDefinition> modelsByName) {

        if (relationField.getRelation() == null || !"ManyToMany".equals(relationField.getRelation().getType())) {
            throw new IllegalArgumentException(
                String.format("Field '%s' is not a many-to-many relation field", relationField.getName())
            );
        }

        final String joinTable = relationField.getRelation().getJoinTable().getName();
        if (!StringUtils.isNotBlank(joinTable)) {
            throw new IllegalArgumentException(
                String.format("Many-to-many relation field '%s' has no join table", relationField.getName())
            );
        }
        final String leftTable = owner.getStorageName();
        final FieldDefinition ownerPk = FieldUtils.extractIdField(owner.getFields());
        final String leftPkCol = ModelNameUtils.toSnakeCase(ownerPk.getName());
        final String leftSqlType = columnSqlType(ownerPk, db);
        final String leftJoinCol = relationField.getRelation().getJoinTable().getJoinColumn();

        final String targetName = relationField.getType();
        final ModelDefinition target = modelsByName.get(targetName);
        final String rightTable = target.getStorageName();
        final FieldDefinition targetPk = FieldUtils.extractIdField(target.getFields());
        final String rightPkCol = ModelNameUtils.toSnakeCase(targetPk.getName());
        final String rightSqlType = columnSqlType(targetPk, db);

        final String rightJoinCol = relationField.getRelation().getJoinTable().getInverseJoinColumn();

        final Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("joinTable", joinTable);
        ctx.put("left", Map.of(
                "column", leftJoinCol,
                "sqlType", leftSqlType,
                "table",  leftTable,
                "pkColumn", leftPkCol
        ));
        ctx.put("right", Map.of(
                "column", rightJoinCol,
                "sqlType", rightSqlType,
                "table",  rightTable,
                "pkColumn", rightPkCol
        ));

        return ctx;
    }

    /**
     * Loads the migration state from the given base directory. If no migration state is found, a new empty one is created.
     *
     * @param baseDir the base directory in which to look for the migration state
     * @return the loaded migration state if found, otherwise a new empty one
     * @throws RuntimeException if an exception occurs while loading the migration state
     */
    public static MigrationState loadOrEmpty(final String baseDir) {
        final Path migrationStatePath = Paths.get(baseDir, ".crud-generator", "migration-state.json");
        try {
            if (Files.exists(migrationStatePath)) {
                return OBJECT_MAPPER.readValue(Files.readAllBytes(migrationStatePath), MigrationState.class);
            }

            return new MigrationState(
                    "1.0",
                    0,
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        } catch (final IOException e) {
            throw new RuntimeException(
                String.format(
                    "Failed to load migration state from '%s': %s", migrationStatePath, e.getMessage()
                ),
                e
            );
        }
    }

    /**
     * Saves the given migration state to the given base directory.
     *
     * @param baseDir the base directory to which to save the migration state
     * @param migrationState the migration state to save
     */
    public static void save(final String baseDir, final MigrationState migrationState) {
        final Path migrationStatePath = Paths.get(baseDir, CRUD_GENERATOR_DIR, MIGRATION_STATE_FILE);
        try {
            final Path dir = migrationStatePath.getParent();
            if (dir != null && Files.notExists(dir)) {
                Files.createDirectories(dir);
            }
            Files.write(
                migrationStatePath,
                OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(migrationState),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (final IOException e) {
            throw new RuntimeException(
                String.format(
                    "Failed to save migration state to '%s': %s", migrationStatePath, e.getMessage()
                ),
                e
            );
        }
    }

}
