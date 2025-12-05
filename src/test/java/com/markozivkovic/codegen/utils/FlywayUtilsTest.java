package com.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.markozivkovic.codegen.models.AuditDefinition;
import com.markozivkovic.codegen.models.AuditDefinition.AuditTypeEnum;
import com.markozivkovic.codegen.models.ColumnDefinition;
import com.markozivkovic.codegen.models.CrudConfiguration.DatabaseType;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.IdDefinition;
import com.markozivkovic.codegen.models.IdDefinition.IdStrategyEnum;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.RelationDefinition;
import com.markozivkovic.codegen.models.RelationDefinition.JoinTableDefinition;
import com.markozivkovic.codegen.models.flyway.MigrationState;

class FlywayUtilsTest {

    @TempDir
    Path tempDir;

    private FieldDefinition manyToManyField(final String name, final String targetType,
                                            final String joinTableName,
                                            final String joinColumn,
                                            final String inverseJoinColumn) {
        final JoinTableDefinition jt = new JoinTableDefinition();
        jt.setName(joinTableName);
        jt.setJoinColumn(joinColumn);
        jt.setInverseJoinColumn(inverseJoinColumn);

        final RelationDefinition rel = new RelationDefinition();
        rel.setType("ManyToMany");
        rel.setJoinTable(jt);

        final FieldDefinition f = new FieldDefinition();
        f.setName(name);
        f.setType(targetType);
        f.setRelation(rel);
        return f;
    }

    private FieldDefinition fieldWithNameTypeAndId(final String name, final String type, final boolean isId) {
        final FieldDefinition field = new FieldDefinition();
        field.setName(name);
        field.setType(type);
        if (isId) {
            IdDefinition id = new IdDefinition();
            id.setStrategy(IdStrategyEnum.IDENTITY);
            field.setId(id);
        }
        return field;
    }

    private FieldDefinition fieldWithType(final String type) {
        
        final FieldDefinition field = new FieldDefinition();
        field.setType(type);
        return field;
    }

    private FieldDefinition fieldWithTypeAndLength(final String type, final Integer length) {
        final FieldDefinition field = new FieldDefinition();
        field.setType(type);
        if (length != null) {
            final ColumnDefinition column = new ColumnDefinition();
            column.setLength(length);
            field.setColumn(column);
        }
        return field;
    }

    private FieldDefinition fieldWithIdAndStrategy(final String name, final String type, final IdStrategyEnum strategy,
                final String sequenceName) {
        
        final FieldDefinition field = new FieldDefinition();
        field.setName(name);
        field.setType(type);

        final IdDefinition id = new IdDefinition();
        id.setStrategy(strategy);
        id.setSequenceName(sequenceName);
        field.setId(id);

        return field;
    }

    private FieldDefinition fieldWithRelation(final String name, final String type, final String relationType,
                final String joinColumn) {
        
        final RelationDefinition relation = new RelationDefinition();
        relation.setType(relationType);
        relation.setJoinColumn(joinColumn);

        final FieldDefinition field = new FieldDefinition();
        field.setName(name);
        field.setType(type);
        field.setRelation(relation);
        return field;
    }

    private ModelDefinition model(final String name, final String storageName, final List<FieldDefinition> fields) {
        
        final ModelDefinition m = new ModelDefinition();
        m.setName(name);
        m.setStorageName(storageName);
        m.setFields(fields);
        return m;
    }
    
    @Test
    @DisplayName("columnSqlType returns VARCHAR(255) for null type on PostgreSQL and MySQL, NVARCHAR(255) on MSSQL")
    void columnSqlType_shouldReturnDefaultVarchar_whenTypeIsNull() {
        
        final FieldDefinition field = fieldWithType(null);

        final String pg = FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL);
        final String my = FlywayUtils.columnSqlType(field, DatabaseType.MYSQL);
        final String ms = FlywayUtils.columnSqlType(field, DatabaseType.MSSQL);

        assertEquals("VARCHAR(255)", pg);
        assertEquals("VARCHAR(255)", my);
        assertEquals("NVARCHAR(255)", ms);
    }

    @Test
    @DisplayName("columnSqlType maps Long to BIGINT for all databases")
    void columnSqlType_shouldMapLongToBigint() {
        
        final FieldDefinition field = fieldWithType("Long");

        assertEquals("BIGINT", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("BIGINT", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("BIGINT", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("columnSqlType maps Integer to INTEGER for PostgreSQL/MySQL and INT for MSSQL")
    void columnSqlType_shouldMapIntegerCorrectlyPerDb() {
        
        final FieldDefinition field = fieldWithType("Integer");

        assertEquals("INTEGER", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("INTEGER", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("INT", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("columnSqlType maps UUID using database-specific uuidType")
    void columnSqlType_shouldMapUuidPerDb() {
        
        final FieldDefinition field = fieldWithType("UUID");

        assertEquals("UUID", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("BINARY(16)", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("UNIQUEIDENTIFIER", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("columnSqlType maps LocalDate to DATE")
    void columnSqlType_shouldMapLocalDate() {
        
        final FieldDefinition field = fieldWithType("LocalDate");

        assertEquals("DATE", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("DATE", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("DATE", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("columnSqlType maps LocalDateTime using localDateTimeType per database")
    void columnSqlType_shouldMapLocalDateTimePerDb() {
        
        final FieldDefinition field = fieldWithType("LocalDateTime");

        assertEquals("TIMESTAMP", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("DATETIME(6)", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("DATETIME2(6)", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("columnSqlType maps Instant using instantType per database")
    void columnSqlType_shouldMapInstantPerDb() {
        
        final FieldDefinition field = fieldWithType("Instant");

        assertEquals("TIMESTAMP WITH TIME ZONE", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("DATETIME(6)", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("DATETIME2(6)", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("columnSqlType maps Enum to VARCHAR(255) / NVARCHAR(255)")
    void columnSqlType_shouldMapEnumToVarchar255() {
        
        final FieldDefinition field = fieldWithType("Enum");

        assertEquals("VARCHAR(255)", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("VARCHAR(255)", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("NVARCHAR(255)", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("columnSqlType maps String without length to default 255")
    void columnSqlType_shouldUseDefaultLengthForStringWithoutColumn() {
        
        final FieldDefinition field = fieldWithType("String");

        final String pg = FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL);
        final String my = FlywayUtils.columnSqlType(field, DatabaseType.MYSQL);
        final String ms = FlywayUtils.columnSqlType(field, DatabaseType.MSSQL);

        assertEquals("VARCHAR(255)", pg);
        assertEquals("VARCHAR(255)", my);
        assertEquals("NVARCHAR(255)", ms);
    }

    @Test
    @DisplayName("columnSqlType maps String with column length to that length")
    void columnSqlType_shouldUseCustomLengthForString() {
        
        final FieldDefinition field = fieldWithTypeAndLength("String", 100);

        final String pg = FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL);
        final String my = FlywayUtils.columnSqlType(field, DatabaseType.MYSQL);
        final String ms = FlywayUtils.columnSqlType(field, DatabaseType.MSSQL);

        assertEquals("VARCHAR(100)", pg);
        assertEquals("VARCHAR(100)", my);
        assertEquals("NVARCHAR(100)", ms);
    }

    @Test
    @DisplayName("columnSqlType maps Boolean to BOOLEAN for PostgreSQL/MySQL and BIT for MSSQL")
    void columnSqlType_shouldMapBooleanPerDb() {
        
        final FieldDefinition field = fieldWithType("Boolean");

        assertEquals("BOOLEAN", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("BOOLEAN", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("BIT", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("columnSqlType maps Double to DOUBLE for MySQL and DOUBLE PRECISION for other databases")
    void columnSqlType_shouldMapDoublePerDb() {
        
        final FieldDefinition field = fieldWithType("Double");

        assertEquals("DOUBLE PRECISION", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("DOUBLE", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("DOUBLE PRECISION", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("columnSqlType maps Float to REAL")
    void columnSqlType_shouldMapFloatToReal() {
        
        final FieldDefinition field = fieldWithType("Float");

        assertEquals("REAL", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("REAL", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("REAL", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("columnSqlType maps BigDecimal using decimalDefault per database")
    void columnSqlType_shouldMapBigDecimalPerDb() {
        
        final FieldDefinition field = fieldWithType("BigDecimal");

        assertEquals("NUMERIC(19,2)", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("DECIMAL(19,2)", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("DECIMAL(19,2)", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("columnSqlType maps Byte and Short to SMALLINT")
    void columnSqlType_shouldMapByteAndShortToSmallint() {
        
        final FieldDefinition byteField = fieldWithType("Byte");
        final FieldDefinition shortField = fieldWithType("Short");

        assertEquals("SMALLINT", FlywayUtils.columnSqlType(byteField, DatabaseType.POSTGRESQL));
        assertEquals("SMALLINT", FlywayUtils.columnSqlType(shortField, DatabaseType.POSTGRESQL));
    }

    @Test
    @DisplayName("columnSqlType maps Char and Character to CHAR(1)")
    void columnSqlType_shouldMapCharToChar1() {
        
        final FieldDefinition f1 = fieldWithType("Char");
        final FieldDefinition f2 = fieldWithType("Character");

        assertEquals("CHAR(1)", FlywayUtils.columnSqlType(f1, DatabaseType.POSTGRESQL));
        assertEquals("CHAR(1)", FlywayUtils.columnSqlType(f2, DatabaseType.POSTGRESQL));
    }

    @Test
    @DisplayName("columnSqlType maps JSON[...] to database-specific JSON type")
    void columnSqlType_shouldMapJsonPerDb() {
        
        final FieldDefinition field = fieldWithType("JSON[Address]");

        assertEquals("JSONB", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("JSON", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("NVARCHAR(MAX)", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("columnSqlType returns BIGINT for unknown custom types")
    void columnSqlType_shouldReturnBigintForUnknownType() {
        
        final FieldDefinition field = fieldWithType("SomeCustomType");

        assertEquals("BIGINT", FlywayUtils.columnSqlType(field, DatabaseType.POSTGRESQL));
        assertEquals("BIGINT", FlywayUtils.columnSqlType(field, DatabaseType.MYSQL));
        assertEquals("BIGINT", FlywayUtils.columnSqlType(field, DatabaseType.MSSQL));
    }

    @Test
    @DisplayName("identityDecorateIfNeeded returns null when ID definition is missing or strategy is null")
    void identityDecorateIfNeeded_shouldReturnNull_whenNoIdOrStrategy() {
        
        final FieldDefinition noId = new FieldDefinition();
        noId.setType("Long");

        final FieldDefinition withIdNoStrategy = new FieldDefinition();
        withIdNoStrategy.setType("Long");
        withIdNoStrategy.setId(new IdDefinition());

        assertNull(FlywayUtils.identityDecorateIfNeeded(DatabaseType.POSTGRESQL, noId, "test"));
        assertNull(FlywayUtils.identityDecorateIfNeeded(DatabaseType.POSTGRESQL, withIdNoStrategy, "test"));
    }

    @Test
    @DisplayName("identityDecorateIfNeeded returns proper auto-increment definition for IDENTITY and AUTO")
    void identityDecorateIfNeeded_shouldReturnIdentityForIdentityAndAuto() {
        
        final FieldDefinition identityField = fieldWithIdAndStrategy("id", "Long", IdStrategyEnum.IDENTITY, null);
        final FieldDefinition autoField = fieldWithIdAndStrategy("id", "Long", IdStrategyEnum.AUTO, null);

        assertEquals("BIGINT GENERATED BY DEFAULT AS IDENTITY",
                FlywayUtils.identityDecorateIfNeeded(DatabaseType.POSTGRESQL, identityField, "t"));
        assertEquals("BIGINT AUTO_INCREMENT",
                FlywayUtils.identityDecorateIfNeeded(DatabaseType.MYSQL, identityField, "t"));
        assertEquals("BIGINT IDENTITY(1,1)",
                FlywayUtils.identityDecorateIfNeeded(DatabaseType.MSSQL, identityField, "t"));

        assertEquals("BIGINT GENERATED BY DEFAULT AS IDENTITY",
                FlywayUtils.identityDecorateIfNeeded(DatabaseType.POSTGRESQL, autoField, "t"));
        assertEquals("BIGINT AUTO_INCREMENT",
                FlywayUtils.identityDecorateIfNeeded(DatabaseType.MYSQL, autoField, "t"));
        assertEquals("BIGINT IDENTITY(1,1)",
                FlywayUtils.identityDecorateIfNeeded(DatabaseType.MSSQL, autoField, "t"));
    }

    @Test
    @DisplayName("identityDecorateIfNeeded uses given sequence name for SEQUENCE strategy")
    void identityDecorateIfNeeded_shouldUseCustomSequence_forSequenceStrategy() {
        
        final FieldDefinition seqField = fieldWithIdAndStrategy("id", "Long", IdStrategyEnum.SEQUENCE, "custom_seq");

        assertEquals("BIGINT DEFAULT NEXTVAL('custom_seq')",
                FlywayUtils.identityDecorateIfNeeded(DatabaseType.POSTGRESQL, seqField, "my_table"));
        assertEquals("BIGINT",
                FlywayUtils.identityDecorateIfNeeded(DatabaseType.MYSQL, seqField, "my_table"));
        assertEquals("BIGINT DEFAULT NEXT VALUE FOR custom_seq",
                FlywayUtils.identityDecorateIfNeeded(DatabaseType.MSSQL, seqField, "my_table"));
    }

    @Test
    @DisplayName("identityDecorateIfNeeded uses default '<table>_id_seq' when sequence name is blank or null")
    void identityDecorateIfNeeded_shouldUseDefaultSequenceName_whenSequenceNameMissing() {
        
        final FieldDefinition seqFieldNull = fieldWithIdAndStrategy("id", "Long", IdStrategyEnum.SEQUENCE, null);
        final FieldDefinition seqFieldBlank = fieldWithIdAndStrategy("id", "Long", IdStrategyEnum.SEQUENCE, "   ");

        assertEquals("BIGINT DEFAULT NEXTVAL('orders_id_seq')",
                FlywayUtils.identityDecorateIfNeeded(DatabaseType.POSTGRESQL, seqFieldNull, "orders"));
        assertEquals("BIGINT DEFAULT NEXTVAL('orders_id_seq')",
                FlywayUtils.identityDecorateIfNeeded(DatabaseType.POSTGRESQL, seqFieldBlank, "orders"));

        assertEquals("BIGINT DEFAULT NEXT VALUE FOR orders_id_seq",
                FlywayUtils.identityDecorateIfNeeded(DatabaseType.MSSQL, seqFieldNull, "orders"));
    }

    @Test
    @DisplayName("identityDecorateIfNeeded returns null for TABLE strategy and other unsupported strategies")
    void identityDecorateIfNeeded_shouldReturnNull_forTableStrategy() {
        
        final FieldDefinition tableField = fieldWithIdAndStrategy("id", "Long", IdStrategyEnum.TABLE, null);

        assertNull(FlywayUtils.identityDecorateIfNeeded(DatabaseType.POSTGRESQL, tableField, "t"));
        assertNull(FlywayUtils.identityDecorateIfNeeded(DatabaseType.MYSQL, tableField, "t"));
        assertNull(FlywayUtils.identityDecorateIfNeeded(DatabaseType.MSSQL, tableField, "t"));
    }

    @Test
    @DisplayName("toForeignKeysContext returns empty context when model has no to-one relations")
    void toForeignKeysContext_shouldReturnEmpty_whenNoToOneRelations() {
        
        final FieldDefinition f1 = new FieldDefinition();
        f1.setName("id");
        final IdDefinition id = new IdDefinition();
        id.setStrategy(IdStrategyEnum.IDENTITY);
        f1.setId(id);

        final FieldDefinition f2 = new FieldDefinition();
        f2.setName("name");

        final ModelDefinition model = model("Order", "order", List.of(f1, f2));
        final Map<String, ModelDefinition> modelsByName = new LinkedHashMap<>();

        final Map<String, Object> ctx = FlywayUtils.toForeignKeysContext(model, modelsByName);

        assertTrue(ctx.isEmpty());
    }

    @Test
    @DisplayName("toForeignKeysContext builds foreign key context with default column and ref names")
    void toForeignKeysContext_shouldBuildFkContext_withDefaults() {
        
        final FieldDefinition orderId = new FieldDefinition();
        orderId.setName("id");
        final IdDefinition orderIdDef = new IdDefinition();
        orderIdDef.setStrategy(IdStrategyEnum.IDENTITY);
        orderId.setId(orderIdDef);

        final FieldDefinition customerField = fieldWithRelation("customer", "Customer", "ManyToOne", null);
        final ModelDefinition orderModel = model("Order", "order", List.of(orderId, customerField));

        final FieldDefinition customerId = new FieldDefinition();
        customerId.setName("id");
        customerId.setType("Long");
        final IdDefinition customerIdDef = new IdDefinition();
        customerIdDef.setStrategy(IdStrategyEnum.IDENTITY);
        customerId.setId(customerIdDef);

        final ModelDefinition customerModel = model("Customer", "customer_table", List.of(customerId));

        final Map<String, ModelDefinition> modelsByName = new LinkedHashMap<>();
        modelsByName.put("Customer", customerModel);

        final Map<String, Object> ctx = FlywayUtils.toForeignKeysContext(orderModel, modelsByName);

        assertFalse(ctx.isEmpty());
        assertTrue(ctx.containsKey("fks"));

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> fks = (List<Map<String, Object>>) ctx.get("fks");
        assertEquals(1, fks.size());

        final Map<String, Object> fk = fks.get(0);
        assertEquals("order", fk.get("table"));
        assertEquals("fk_order_customer_id", fk.get("name"));
        assertEquals("customer_id", fk.get("column"));
        assertEquals("customer_table", fk.get("refTable"));
        assertEquals("id", fk.get("refColumn"));
    }

    @Test
    @DisplayName("toForeignKeysContext uses explicit join column and default table/ref names when storageName is missing")
    void toForeignKeysContext_shouldUseExplicitJoinColumn_andDefaultStorageName() {
        
        final FieldDefinition orderId = new FieldDefinition();
        orderId.setName("id");
        final IdDefinition orderIdDef = new IdDefinition();
        orderIdDef.setStrategy(IdStrategyEnum.IDENTITY);
        orderId.setId(orderIdDef);

        final FieldDefinition customerField = fieldWithRelation("customer", "Customer", "ManyToOne", "customer_fk");

        final ModelDefinition orderModel = model("Order", "order", List.of(orderId, customerField));

        final FieldDefinition customerPk = new FieldDefinition();
        customerPk.setName("customerId");
        customerPk.setType("Long");
        final IdDefinition customerPkId = new IdDefinition();
        customerPkId.setStrategy(IdStrategyEnum.IDENTITY);
        customerPk.setId(customerPkId);

        final ModelDefinition customerModel = model("Customer", null, List.of(customerPk));

        final Map<String, ModelDefinition> modelsByName = new LinkedHashMap<>();
        modelsByName.put("Customer", customerModel);

        final Map<String, Object> ctx = FlywayUtils.toForeignKeysContext(orderModel, modelsByName);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> fks = (List<Map<String, Object>>) ctx.get("fks");
        assertEquals(1, fks.size());

        final Map<String, Object> fk = fks.get(0);
        assertEquals("order", fk.get("table"));
        assertEquals("fk_order_customer_fk", fk.get("name"));
        assertEquals("customer_fk", fk.get("column"));
        assertEquals("customer", fk.get("refTable"));
        assertEquals("customer_id", fk.get("refColumn"));
    }

    @Test
    @DisplayName("collectReverseOneToManyExtras returns empty map when there are no OneToMany relations")
    void collectReverseOneToManyExtras_shouldReturnEmpty_whenNoOneToMany() {

        final FieldDefinition idField = new FieldDefinition();
        idField.setName("id");
        final IdDefinition id = new IdDefinition();
        id.setStrategy(IdStrategyEnum.IDENTITY);
        idField.setId(id);

        final FieldDefinition nameField = new FieldDefinition();
        nameField.setName("name");

        final ModelDefinition parent = model("Order", "order", List.of(idField, nameField));

        final Map<String, ModelDefinition> modelsByName = new LinkedHashMap<>();
        modelsByName.put("Order", parent);

        final Map<String, List<Map<String, Object>>> result =
                FlywayUtils.collectReverseOneToManyExtras(List.of(parent), DatabaseType.POSTGRESQL, modelsByName);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("collectReverseOneToManyExtras adds FK column with default '<parent>_id' name for OneToMany relation")
    void collectReverseOneToManyExtras_shouldCreateFkColumn_withDefaultName() {

        final FieldDefinition orderId = new FieldDefinition();
        orderId.setName("id");
        orderId.setType("Long");
        final IdDefinition orderIdDef = new IdDefinition();
        orderIdDef.setStrategy(IdStrategyEnum.IDENTITY);
        orderId.setId(orderIdDef);

        final FieldDefinition itemsField = fieldWithRelation("items", "OrderItem", "OneToMany", null);
        final ModelDefinition parent = model("Order", "order", List.of(orderId, itemsField));

        final FieldDefinition itemId = new FieldDefinition();
        itemId.setName("id");
        itemId.setType("Long");
        final IdDefinition itemIdDef = new IdDefinition();
        itemIdDef.setStrategy(IdStrategyEnum.IDENTITY);
        itemId.setId(itemIdDef);

        final ModelDefinition child = model("OrderItem", "order_item", List.of(itemId));

        final Map<String, ModelDefinition> modelsByName = new LinkedHashMap<>();
        modelsByName.put("Order", parent);
        modelsByName.put("OrderItem", child);

        final Map<String, List<Map<String, Object>>> result =
                FlywayUtils.collectReverseOneToManyExtras(List.of(parent), DatabaseType.POSTGRESQL, modelsByName);

        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("order_item"));

        final List<Map<String, Object>> cols = result.get("order_item");
        assertEquals(1, cols.size());

        final Map<String, Object> col = cols.get(0);
        assertEquals("order_id", col.get("name"));
        assertEquals("BIGINT", col.get("sqlType"));
        assertEquals(true, col.get("nullable"));
        assertEquals(false, col.get("unique"));
        assertEquals(false, col.get("isPk"));
    }

    @Test
    @DisplayName("collectReverseOneToManyExtras uses explicit joinColumn and avoids duplicates for same child/column")
    void collectReverseOneToManyExtras_shouldUseExplicitJoinColumn_andAvoidDuplicates() {

        final FieldDefinition orderId = new FieldDefinition();
        orderId.setName("id");
        orderId.setType("Long");
        final IdDefinition orderIdDef = new IdDefinition();
        orderIdDef.setStrategy(IdStrategyEnum.IDENTITY);
        orderId.setId(orderIdDef);

        final FieldDefinition itemsField1 = fieldWithRelation("items", "OrderItem", "OneToMany", "order_fk");
        final FieldDefinition itemsField2 = fieldWithRelation("otherItems", "OrderItem", "OneToMany", "order_fk");

        final ModelDefinition parent = model("Order", "order", List.of(orderId, itemsField1, itemsField2));

        final FieldDefinition itemId = new FieldDefinition();
        itemId.setName("id");
        itemId.setType("Long");
        final IdDefinition itemIdDef = new IdDefinition();
        itemIdDef.setStrategy(IdStrategyEnum.IDENTITY);
        itemId.setId(itemIdDef);

        final ModelDefinition child = model("OrderItem", "order_item", List.of(itemId));

        final Map<String, ModelDefinition> modelsByName = new LinkedHashMap<>();
        modelsByName.put("Order", parent);
        modelsByName.put("OrderItem", child);

        final Map<String, List<Map<String, Object>>> result =
                FlywayUtils.collectReverseOneToManyExtras(List.of(parent), DatabaseType.POSTGRESQL, modelsByName);

        final List<Map<String, Object>> cols = result.get("order_item");
        assertEquals(1, cols.size(), "Expected no duplicate FK columns for same name");

        final Map<String, Object> col = cols.get(0);
        assertEquals("order_fk", col.get("name"));
    }

    @Test
    @DisplayName("collectReverseOneToManyExtras skips relations when child model is missing or has no storageName")
    void collectReverseOneToManyExtras_shouldSkip_whenChildMissingOrNoStorageName() {

        final FieldDefinition orderId = new FieldDefinition();
        orderId.setName("id");
        orderId.setType("Long");
        final IdDefinition orderIdDef = new IdDefinition();
        orderIdDef.setStrategy(IdStrategyEnum.IDENTITY);
        orderId.setId(orderIdDef);

        final FieldDefinition itemsField = fieldWithRelation("items", "OrderItem", "OneToMany", null);
        final ModelDefinition parent = model("Order", "order", List.of(orderId, itemsField));
        final ModelDefinition childNoStorage = model("OrderItem", null, List.of());

        final Map<String, ModelDefinition> modelsByName = new LinkedHashMap<>();
        modelsByName.put("Order", parent);
        modelsByName.put("OrderItem", childNoStorage);

        final Map<String, List<Map<String, Object>>> result =
                FlywayUtils.collectReverseOneToManyExtras(List.of(parent), DatabaseType.POSTGRESQL, modelsByName);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toCreateTableContext builds basic column metadata, PK, checks and audit context")
    void toCreateTableContext_shouldBuildBasicContext() {

        final FieldDefinition idField = new FieldDefinition();
        idField.setName("id");
        idField.setType("Long");
        final IdDefinition idDef = new IdDefinition();
        idDef.setStrategy(IdStrategyEnum.IDENTITY);
        idField.setId(idDef);

        final ColumnDefinition idCol = new ColumnDefinition();
        idCol.setNullable(false);
        idField.setColumn(idCol);

        final FieldDefinition nameField = new FieldDefinition();
        nameField.setName("name");
        nameField.setType("String");
        final ColumnDefinition nameCol = new ColumnDefinition();
        nameCol.setNullable(false);
        nameCol.setUnique(true);
        nameCol.setLength(100);
        nameField.setColumn(nameCol);

        final FieldDefinition statusField = new FieldDefinition();
        statusField.setName("status");
        statusField.setType("Enum");
        statusField.setValues(List.of("NEW", "PAID"));

        final ModelDefinition model = model("Order", "order", List.of(idField, nameField, statusField));
        final Map<String, ModelDefinition> modelsByName = new LinkedHashMap<>();

        final Map<String, Object> ctx = FlywayUtils.toCreateTableContext(model, DatabaseType.POSTGRESQL, modelsByName, List.of());

        assertEquals("order", ctx.get("tableName"));

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> cols = (List<Map<String, Object>>) ctx.get("columns");
        assertEquals(3, cols.size());

        final Map<String, Object> idColMeta = cols.stream()
                .filter(c -> "id".equals(c.get("name")))
                .findFirst()
                .orElseThrow();

        assertEquals("BIGINT GENERATED BY DEFAULT AS IDENTITY", idColMeta.get("sqlType"));
        assertEquals(false, idColMeta.get("nullable"));
        assertEquals(false, idColMeta.get("unique"));
        assertEquals(true, idColMeta.get("isPk"));

        Map<String, Object> nameColMeta = cols.stream()
                .filter(c -> "name".equals(c.get("name")))
                .findFirst()
                .orElseThrow();

        assertEquals("VARCHAR(100)", nameColMeta.get("sqlType"));
        assertEquals(false, nameColMeta.get("nullable"));
        assertEquals(true, nameColMeta.get("unique"));
        assertEquals(false, nameColMeta.get("isPk"));

        @SuppressWarnings("unchecked")
        final List<Map<String, String>> checks = (List<Map<String, String>>) ctx.get("checks");
        assertEquals(1, checks.size());
        final Map<String, String> ck = checks.get(0);
        assertEquals("ck_order_status_enum", ck.get("name"));
        assertEquals("status IN ('NEW', 'PAID')", ck.get("expr"));

        assertEquals("id", ctx.get("pkColumns"));
        assertEquals(false, ctx.get("auditEnabled"));
        assertEquals("TIMESTAMP WITH TIME ZONE", ctx.get("auditCreatedType"));
        assertEquals("TIMESTAMP WITH TIME ZONE", ctx.get("auditUpdatedType"));
        assertEquals("now()", ctx.get("auditNowExpr"));
    }

    @Test
    @DisplayName("toCreateTableContext merges extraColumnsForThisTable and avoids duplicates")
    void toCreateTableContext_shouldMergeExtraColumns_andAvoidDuplicates() {

        final FieldDefinition idField = new FieldDefinition();
        idField.setName("id");
        idField.setType("Long");
        final IdDefinition idDef = new IdDefinition();
        idDef.setStrategy(IdStrategyEnum.IDENTITY);
        idField.setId(idDef);

        final ModelDefinition model = model("Order", "order", List.of(idField));

        final Map<String, Object> extra1 = new LinkedHashMap<>();
        extra1.put("name", "tenant_id");
        extra1.put("sqlType", "BIGINT");
        extra1.put("nullable", true);
        extra1.put("unique", false);
        extra1.put("isPk", false);

        final Map<String, Object> extraDuplicate = new LinkedHashMap<>(extra1);
        final List<Map<String, Object>> extras = List.of(extra1, extraDuplicate);

        final Map<String, Object> ctx = FlywayUtils.toCreateTableContext(model, DatabaseType.POSTGRESQL, Map.of(), extras);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> cols = (List<Map<String, Object>>) ctx.get("columns");

        assertEquals(2, cols.size());
        assertTrue(cols.stream().anyMatch(c -> "tenant_id".equals(c.get("name"))));
    }

    @Test
    @DisplayName("toCreateTableContext fills audit context when audit is enabled")
    void toCreateTableContext_shouldFillAuditContext_whenAuditEnabled() {

        final FieldDefinition idField = new FieldDefinition();
        idField.setName("id");
        idField.setType("Long");
        IdDefinition idDef = new IdDefinition();
        idDef.setStrategy(IdStrategyEnum.IDENTITY);
        idField.setId(idDef);

        final ModelDefinition model = model("Order", "order", List.of(idField));

        final AuditDefinition audit = new AuditDefinition();
        audit.setEnabled(true);
        audit.setType(AuditTypeEnum.LOCAL_DATE_TIME);
        model.setAudit(audit);

        final Map<String, Object> ctx = FlywayUtils.toCreateTableContext(model, DatabaseType.POSTGRESQL, Map.of(), List.of());

        assertEquals(true, ctx.get("auditEnabled"));
        assertEquals("TIMESTAMP", ctx.get("auditCreatedType"));
        assertEquals("TIMESTAMP", ctx.get("auditUpdatedType"));
        assertEquals("now()", ctx.get("auditNowExpr"));
    }

    @Test
    @DisplayName("toJoinTableContext throws IllegalArgumentException when field is not ManyToMany")
    void toJoinTableContext_shouldThrow_whenFieldIsNotManyToMany() {
        final ModelDefinition owner = model("User", "user", List.of());
        final FieldDefinition relFieldNoRelation = new FieldDefinition();
        relFieldNoRelation.setName("roles");

        final IllegalArgumentException ex1 = assertThrows(
                IllegalArgumentException.class,
                () -> FlywayUtils.toJoinTableContext(owner, relFieldNoRelation, DatabaseType.POSTGRESQL, Map.of())
        );
        assertTrue(ex1.getMessage().contains("is not a many-to-many relation field"));

        final FieldDefinition relFieldOtherType = fieldWithRelation("roles", "Role", "OneToMany", null);

        final IllegalArgumentException ex2 = assertThrows(
                IllegalArgumentException.class,
                () -> FlywayUtils.toJoinTableContext(owner, relFieldOtherType, DatabaseType.POSTGRESQL, Map.of())
        );
        assertTrue(ex2.getMessage().contains("is not a many-to-many relation field"));
    }

    @Test
    @DisplayName("toJoinTableContext throws IllegalArgumentException when join table name is missing or blank")
    void toJoinTableContext_shouldThrow_whenJoinTableNameMissing() {
        
        final ModelDefinition owner = model("User", "user", List.of(
                fieldWithNameTypeAndId("id", "Long", true)
        ));

        final JoinTableDefinition jt = new JoinTableDefinition();
        jt.setName("  ");
        jt.setJoinColumn("user_id");
        jt.setInverseJoinColumn("role_id");

        final RelationDefinition rel = new RelationDefinition();
        rel.setType("ManyToMany");
        rel.setJoinTable(jt);

        final FieldDefinition relField = new FieldDefinition();
        relField.setName("roles");
        relField.setType("Role");
        relField.setRelation(rel);

        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FlywayUtils.toJoinTableContext(owner, relField, DatabaseType.POSTGRESQL, Map.of())
        );

        assertTrue(ex.getMessage().contains("has no join table"));
    }

    @Test
    @DisplayName("toJoinTableContext builds correct join table context for ManyToMany relation")
    void toJoinTableContext_shouldBuildContext_forManyToManyRelation() {
        
        final FieldDefinition userId = fieldWithNameTypeAndId("id", "Long", true);
        final ModelDefinition user = model("User", "app_user", List.of(userId));
        final FieldDefinition roleId = fieldWithNameTypeAndId("id", "UUID", true);
        final ModelDefinition role = model("Role", "role", List.of(roleId));

        final FieldDefinition rolesField = manyToManyField(
                "roles", "Role", "user_roles", "user_id", "role_id"
        );

        final Map<String, ModelDefinition> modelsByName = new LinkedHashMap<>();
        modelsByName.put("Role", role);

        final Map<String, Object> ctx = FlywayUtils.toJoinTableContext(
                user, rolesField, DatabaseType.POSTGRESQL, modelsByName
        );

        assertEquals("user_roles", ctx.get("joinTable"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> left = (Map<String, Object>) ctx.get("left");
        @SuppressWarnings("unchecked")
        final Map<String, Object> right = (Map<String, Object>) ctx.get("right");

        assertEquals("user_id", left.get("column"));
        assertEquals("BIGINT", left.get("sqlType"));
        assertEquals("app_user", left.get("table"));
        assertEquals("id", left.get("pkColumn"));

        assertEquals("role_id", right.get("column"));
        assertEquals("UUID", right.get("sqlType"));
        assertEquals("role", right.get("table"));
        assertEquals("id", right.get("pkColumn"));
    }

    @Test
    @DisplayName("loadOrEmpty returns default MigrationState when file does not exist")
    void loadOrEmpty_shouldReturnDefault_whenFileDoesNotExist() {

        final MigrationState state = FlywayUtils.loadOrEmpty(tempDir.toString());

        assertNotNull(state);
        assertEquals("1.0", state.getGeneratorVersion());
        assertEquals(0, state.getLastScriptVersion());
        assertNotNull(state.getEntities());
        assertTrue(state.getEntities().isEmpty());
    }

    @Test
    @DisplayName("save writes migration-state.json and loadOrEmpty reads the same state back")
    void saveAndLoad_shouldPersistAndReadMigrationState() {

        final MigrationState original = new MigrationState(
                "2.0", 5, new ArrayList<>()
        );

        FlywayUtils.save(tempDir.toString(), original);

        final Path statePath = tempDir
                .resolve(".crud-generator")
                .resolve("migration-state.json");

        assertTrue(Files.exists(statePath), "Expected migration-state.json to be created");

        final  MigrationState loaded = FlywayUtils.loadOrEmpty(tempDir.toString());
        assertNotNull(loaded);
        assertEquals(original, loaded);
    }

    @Test
    @DisplayName("save creates .crud-generator directory if it does not exist")
    void save_shouldCreateCrudGeneratorDirectoryWhenMissing() throws IOException {

        final Path base = tempDir.resolve("subdir");
        Files.createDirectories(base);

        final MigrationState state = new MigrationState(
                "3.0", 1, List.of()
        );

        FlywayUtils.save(base.toString(), state);

        final Path dir  = base.resolve(".crud-generator");
        final Path file = dir.resolve("migration-state.json");

        assertTrue(Files.exists(dir),  "Expected .crud-generator directory to be created");
        assertTrue(Files.exists(file), "Expected migration-state.json to be created");

        final MigrationState loaded = FlywayUtils.loadOrEmpty(base.toString());
        assertEquals(state, loaded);
    }

}
