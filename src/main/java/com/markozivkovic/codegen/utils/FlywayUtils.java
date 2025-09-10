package com.markozivkovic.codegen.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.CrudConfiguration.DatabaseType;
import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.IdDefinition.IdStrategyEnum;
import com.markozivkovic.codegen.model.ModelDefinition;

public class FlywayUtils {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayUtils.class);
    private static final String SHA_256 = "SHA-256";
    private static final String MIGRATIONS = "migrations";
    private static final String MIGRATIONS_JSON = "migrations.json";
    private static final int DEFAULT_VARCHAR = 255;

    private FlywayUtils() {

    }

    /**
     * Calculate the SHA-256 hash of a given string.
     *
     * @param str String to hash
     * @return SHA-256 hash of the given string
     */
    public static String sha256(final String str) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance(SHA_256);
            final byte[] hash = messageDigest.digest(
                str.getBytes(StandardCharsets.UTF_8)
            );
            final StringBuilder sb = new StringBuilder();
            
            Arrays.asList(hash).forEach(b ->
                sb.append(String.format("%02x", b))
            );
            
            return sb.toString();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the given content to a file at the specified output directory with the given
     * name if the file does not exist or if the file exists but the content is different.
     * Logs the operation result.
     *
     * @param outputDir the directory where the file should be written
     * @param content   the content to be written to the file
     * @return true if the file was written, false if the file was not written
     * @throws RuntimeException if an I/O error occurs during file writing
     */
    public static boolean writeIfNotExistsOrDifferent(final String outputDir, final String content) {

        try {
            Files.createDirectories(Paths.get(outputDir, MIGRATIONS));
        } catch (final Exception e) {
            LOGGER.error("Error creating migrations directory", e);
            throw new RuntimeException(e);
        }
        final Path directoryPath = Paths.get(outputDir, MIGRATIONS);
        final Path migrationFile = directoryPath.resolve(MIGRATIONS_JSON);
        
        try {
            if (Files.exists(migrationFile)) {
                final String existing = Files.readString(migrationFile);
                if (existing.equals(content)) {
                    return false;
                }

                Files.writeString(migrationFile, content, StandardCharsets.UTF_8);
            } else {
                Files.writeString(migrationFile, content, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            LOGGER.error("Error reading migration file", e);
            throw new RuntimeException(e);
        }

        return true;
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
            case MYSQL    -> "VARCHAR(" + len + ")";
            case MSSQL    -> "NVARCHAR(" + len + ")";
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
            case MYSQL    -> "DATETIME(6)";
            case MSSQL    -> "DATETIME2(6)";
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
            case MYSQL    -> "DATETIME(6)";
            case MSSQL    -> "DATETIME2(6)";
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
            case MYSQL    -> "DECIMAL(19,2)";
            case MSSQL    -> "DECIMAL(19,2)";
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
            case MYSQL    -> "JSON";
            case MSSQL    -> "NVARCHAR(MAX)";
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
            case MYSQL    -> "BINARY(16)";
            case MSSQL    -> "UNIQUEIDENTIFIER";
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
                case MYSQL    -> "DATETIME(6)";
                case MSSQL    -> "DATETIMEOFFSET(6)";
            };
        }
        return switch (d) {
            case POSTGRESQL -> "TIMESTAMP WITH TIME ZONE";
            case MYSQL    -> "DATETIME(6)";
            case MSSQL    -> "DATETIME2(6)";
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
            case MYSQL    -> "CURRENT_TIMESTAMP(6)";
            case MSSQL    -> "SYSUTCDATETIME()";
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
                final Integer length = (fieldDefinition.getColumn() != null) ?
                        fieldDefinition.getColumn().getLength() : DEFAULT_VARCHAR;
                return varcharOrText(databaseType, length);
            }
            case "Boolean": return databaseType == DatabaseType.MSSQL ? "BIT" : "BOOLEAN";
            case "Double": return databaseType == DatabaseType.MYSQL ? "DOUBLE" : "DOUBLE PRECISION";
            case "Float": return "REAL";
            case "BigDecimal": return decimalDefault(databaseType);
            case "Byte": return "SMALLINT";
            case "Short": return "SMALLINT";
            case "Char": return "CHAR(1)";
            case "Character": return "CHAR(1)";
            default:
                if (t.startsWith("JSON[")) return jsonType(databaseType);
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
            case AUTO:
                return switch (databaseType) {
                    case POSTGRESQL -> "BIGINT GENERATED BY DEFAULT AS IDENTITY";
                    case MYSQL    -> "BIGINT AUTO_INCREMENT";
                    case MSSQL    -> "BIGINT IDENTITY(1,1)";
                };
            case SEQUENCE:
                final String sequence = StringUtils.isNotBlank(fieldDefinition.getId().getSequenceName()) ?
                        fieldDefinition.getId().getSequenceName() : table + "_id_seq";
                return switch (databaseType) {
                    case POSTGRESQL -> String.format("BIGINT DEFAULT NEXTVAL('%s')", sequence);
                    case MYSQL    -> "BIGINT";
                    case MSSQL    -> String.format("BIGINT DEFAULT NEXT VALUE FOR %s", sequence);
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
    static String sequenceDefaultExpr(DatabaseType d, FieldDefinition f, String table) {
        if (f.getId() == null || f.getId().getStrategy() != IdStrategyEnum.SEQUENCE) return null;
        String seq = (f.getId().getSequenceName()!=null && !f.getId().getSequenceName().isBlank())
                ? f.getId().getSequenceName()
                : table + "_id_seq";
        return switch (d) {
            case POSTGRESQL -> "DEFAULT nextval('" + seq + "')";
            case MSSQL    -> "DEFAULT NEXT VALUE FOR " + seq;
            case MYSQL    -> null;
        };
    }
    
    /**
     * Maps a model definition to a Flyway migration context.
     *
     * This method takes a model definition and converts it into a Flyway migration context.
     * This context can then be used to generate the SQL for the underlying database.
     *
     * @param modelDefinition the model definition to be converted into a Flyway migration context
     * @return a Flyway migration context as a map
     */
    public static Map<String, Object> toFlywayProperty(final ModelDefinition modelDefinition, final DatabaseType databaseType) {

        final String tableName = modelDefinition.getStorageName();

        final List<Map<String, Object>> cols = new ArrayList<>();
        final List<Map<String, String>> checks = new ArrayList<>();
        String pkCols = null;

        for (final FieldDefinition field : modelDefinition.getFields()) {

            final Map<String, Object> column = new LinkedHashMap<>();
            final String columnName = toSnakeCase(StringUtils.uncapitalize(field.getName()));
            column.put("name", columnName);
            column.put("sqlType", columnSqlType(field, databaseType));

            final String identityType = identityDecorateIfNeeded(databaseType, field, tableName);
            if (identityType != null) {
                column.put("sqlType", identityType);
            }

            String seqDefault = sequenceDefaultExpr(databaseType, field, tableName);
            if (seqDefault != null) {
                column.put("defaultExpr", seqDefault);
            }

            final boolean nullable = field.getColumn() == null || Boolean.TRUE.equals(field.getColumn().getNullable());
            column.put("nullable", nullable);

            final boolean unique = field.getColumn() != null && Boolean.TRUE.equals(field.getColumn().getUnique());
            column.put("unique", unique);

            final boolean isPk = field.getId() != null;
            column.put("isPk", isPk);
            if (isPk) {
                pkCols = columnName;
            }

            if ("Enum".equals(field.getType()) && field.getValues() != null && !field.getValues().isEmpty()) {
                Map<String,String> ck = new LinkedHashMap<>();
                ck.put("name", "ck_" + tableName + "_" + columnName + "_enum");
                ck.put("expr", columnName + " IN (" +
                        field.getValues().stream().map(v -> "'" + v + "'").collect(java.util.stream.Collectors.joining(", "))
                        + ")");
                checks.add(ck);
            }
            cols.add(column);
        }

        final String auditType;
        final boolean auditEnabled;
        if (Objects.nonNull(modelDefinition.getAudit()) && modelDefinition.getAudit().isEnabled()) {
            auditEnabled = true;
            auditType = AuditUtils.resolveAuditType(modelDefinition.getAudit().getType());
        } else {
            auditEnabled = false;
            auditType = "";
        }

        final Map<String,Object> context = new LinkedHashMap<>();
        context.put("tableName", tableName);
        context.put("columns", cols);
        if (!checks.isEmpty()) {
            context.put("checks", checks);
        }
        if (pkCols != null) {
            context.put("pkColumns", pkCols);
        }
        context.put("auditEnabled", auditEnabled);
        context.put("auditCreatedType", auditType(databaseType, auditType));
        context.put("auditUpdatedType", auditType(databaseType, auditType));
        context.put("auditNowExpr", auditNow(databaseType));

        return context;
    }

    /**
     * Converts a camel-case string to a snake-case string.
     * @param s the string to convert
     * @return the converted string
     */
    public static String toSnakeCase(final String s) {
        return s.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

}
