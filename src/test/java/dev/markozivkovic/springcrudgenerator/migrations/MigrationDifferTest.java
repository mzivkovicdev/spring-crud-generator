package dev.markozivkovic.springcrudgenerator.migrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.markozivkovic.springcrudgenerator.models.flyway.AuditState;
import dev.markozivkovic.springcrudgenerator.models.flyway.ColumnState;
import dev.markozivkovic.springcrudgenerator.models.flyway.EntityState;
import dev.markozivkovic.springcrudgenerator.models.flyway.FkState;
import dev.markozivkovic.springcrudgenerator.models.flyway.SchemaDiff;
import dev.markozivkovic.springcrudgenerator.models.flyway.SchemaDiff.AddedColumn;
import dev.markozivkovic.springcrudgenerator.models.flyway.SchemaDiff.ColumnChange;
import dev.markozivkovic.springcrudgenerator.models.flyway.SchemaDiff.Result;

class MigrationDifferTest {

    @Test
    void fkKey_shouldBuildCompositeKey() {
        final String key = MigrationDiffer.fkKey("user_id", "users", "id");
        assertEquals("user_id->users(id)", key);
    }

    @Test
    void splitCsv_shouldReturnEmptyListForNullOrLiteralNull() throws Exception {
        
        final var method = MigrationDiffer.class.getDeclaredMethod("splitCsv", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        final List<String> nullResult = (List<String>) method.invoke(null, (String) null);
        assertTrue(nullResult.isEmpty());

        @SuppressWarnings("unchecked")
        final List<String> literalNullResult = (List<String>) method.invoke(null, "null");
        assertTrue(literalNullResult.isEmpty());
    }

    @Test
    void splitCsv_shouldSplitTrimAndIgnoreEmpty() throws Exception {
        
        final var method = MigrationDiffer.class.getDeclaredMethod("splitCsv", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        final List<String> result = (List<String>) method.invoke(null, " id ,  name,  ,  created_at ");

        assertEquals(List.of("id", "name", "created_at"), result);
    }

    @Test
    void equalListIgnoreOrder_shouldReturnTrueForSameElementsDifferentOrder() throws Exception {
        
        final var method = MigrationDiffer.class.getDeclaredMethod("equalListIgnoreOrder", List.class, List.class);
        method.setAccessible(true);

        final List<String> firstList = List.of("id", "name");
        final List<String> secondList = List.of("name", "id");

        final boolean equal = (boolean) method.invoke(null, firstList, secondList);
        assertTrue(equal);
    }

    @Test
    void equalListIgnoreOrder_shouldIgnoreDuplicatesBecauseOfSetSemantics() throws Exception {
        
        final var method = MigrationDiffer.class.getDeclaredMethod("equalListIgnoreOrder", List.class, List.class);
        method.setAccessible(true);

        final List<String> firstList = List.of("id", "id", "name");
        final List<String> secondList = List.of("name", "id");

        final boolean equal = (boolean) method.invoke(null, firstList, secondList);
        assertTrue(equal);
    }

    @Test
    void diff_shouldNotMarkPkChangedWhenPkSameIgnoringOrder() {
        
        final EntityState oldState = mock(EntityState.class);
        when(oldState.getColumns()).thenReturn(Collections.emptyMap());
        when(oldState.getFks()).thenReturn(null);
        when(oldState.getPk()).thenReturn(List.of("id", "tenant_id"));

        final Map<String, Object> newCreateCtx = new HashMap<>();
        newCreateCtx.put("columns", Collections.emptyList());
        newCreateCtx.put("pkColumns", "tenant_id, id");

        final Result result = MigrationDiffer.diff(oldState, newCreateCtx);

        assertFalse(result.isPkChanged());
        assertTrue(result.getNewPk().isEmpty());
    }

    @Test
    void diff_shouldMarkPkChangedWhenPkDiffers() {
        
        final EntityState oldState = mock(EntityState.class);
        when(oldState.getColumns()).thenReturn(Collections.emptyMap());
        when(oldState.getFks()).thenReturn(null);
        when(oldState.getPk()).thenReturn(List.of("id"));

        final Map<String, Object> newCreateCtx = new HashMap<>();
        newCreateCtx.put("columns", Collections.emptyList());
        newCreateCtx.put("pkColumns", "id, tenant_id");

        final Result result = MigrationDiffer.diff(oldState, newCreateCtx);

        assertTrue(result.isPkChanged());
        assertEquals(List.of("id", "tenant_id"), result.getNewPk());
    }

    @Test
    void diff_shouldDetectAddedRemovedAndModifiedColumns() {
    
        final ColumnState idOld = mock(ColumnState.class);
        when(idOld.getType()).thenReturn("BIGINT");
        when(idOld.getNullable()).thenReturn(false);
        when(idOld.getUnique()).thenReturn(false);
        when(idOld.getDefaultExpr()).thenReturn("1");

        final ColumnState ageOld = mock(ColumnState.class);
        when(ageOld.getType()).thenReturn("INTEGER");
        when(ageOld.getNullable()).thenReturn(true);
        when(ageOld.getUnique()).thenReturn(false);
        when(ageOld.getDefaultExpr()).thenReturn(null);

        final Map<String, ColumnState> oldCols = new LinkedHashMap<>();
        oldCols.put("id", idOld);
        oldCols.put("age", ageOld);

        final EntityState oldState = mock(EntityState.class);
        when(oldState.getColumns()).thenReturn(oldCols);
        when(oldState.getPk()).thenReturn(Collections.emptyList());
        when(oldState.getFks()).thenReturn(null);

        final Map<String, Object> newIdCol = new HashMap<>();
        newIdCol.put("name", "id");
        newIdCol.put("sqlType", "VARCHAR");
        newIdCol.put("nullable", true);
        newIdCol.put("unique", true);
        newIdCol.put("defaultExpr", "2");

        final Map<String, Object> newNameCol = new HashMap<>();
        newNameCol.put("name", "name");
        newNameCol.put("sqlType", "VARCHAR");
        newNameCol.put("nullable", true);
        newNameCol.put("unique", false);
        newNameCol.put("defaultExpr", null);

        final List<Map<String, Object>> columnsCtx = new ArrayList<>();
        columnsCtx.add(newIdCol);
        columnsCtx.add(newNameCol);

        final Map<String, Object> newCreateCtx = new HashMap<>();
        newCreateCtx.put("columns", columnsCtx);
        newCreateCtx.put("pkColumns", null);
        newCreateCtx.put("fksCtx", null);
        newCreateCtx.put("fks", null);

        final Result result = MigrationDiffer.diff(oldState, newCreateCtx);

        assertEquals(1, result.getAddedColumns().size());
        final AddedColumn added = result.getAddedColumns().get(0);
        assertEquals("name", added.getName());
        assertEquals("VARCHAR", added.getType());
        assertTrue(added.getNullable());
        assertFalse(added.getUnique());
        assertNull(added.getDefaultValue());

        assertEquals(1, result.getRemovedColumns().size());
        assertTrue(result.getRemovedColumns().contains("age"));

        assertEquals(1, result.getModifiedColumns().size());
        final ColumnChange change = result.getModifiedColumns().get(0);
        assertEquals("id", change.getName());
        assertEquals("BIGINT", change.getOldType());
        assertEquals("VARCHAR", change.getNewType());
        assertTrue(change.getTypeChanged());
        assertTrue(change.getNullableChanged());
        assertTrue(change.getUniqueChanged());
        assertTrue(change.getDefaultChanged());
    }

    @Test
    void diff_shouldDetectAddedAndRemovedForeignKeys() {
        
        final FkState oldFk = mock(FkState.class);
        when(oldFk.getColumn()).thenReturn("role_id");
        when(oldFk.getRefTable()).thenReturn("roles");
        when(oldFk.getRefColumn()).thenReturn("id");

        final EntityState oldState = mock(EntityState.class);
        when(oldState.getColumns()).thenReturn(Collections.emptyMap());
        when(oldState.getPk()).thenReturn(Collections.emptyList());
        when(oldState.getFks()).thenReturn(List.of(oldFk));

        final Map<String, Object> newFk = new HashMap<>();
        newFk.put("column", "tenant_id");
        newFk.put("refTable", "tenants");
        newFk.put("refColumn", "id");

        final List<Map<String, Object>> fksList = List.of(newFk);

        final Map<String, Object> fkCtx = new HashMap<>();
        fkCtx.put("fks", fksList);

        final Map<String, Object> newCreateCtx = new HashMap<>();
        newCreateCtx.put("columns", Collections.emptyList());
        newCreateCtx.put("pkColumns", null);
        newCreateCtx.put("fksCtx", fkCtx);

        final Result result = MigrationDiffer.diff(oldState, newCreateCtx);

        assertEquals(1, result.getAddedFks().size());
        final SchemaDiff.FkChange addedFk = result.getAddedFks().get(0);
        assertEquals("tenant_id", addedFk.getColumn());
        assertEquals("tenants", addedFk.getRefTable());
        assertEquals("id", addedFk.getRefColumn());

        assertEquals(1, result.getRemovedFks().size());
        final SchemaDiff.FkChange removedFk = result.getRemovedFks().get(0);
        assertEquals("role_id", removedFk.getColumn());
        assertEquals("roles", removedFk.getRefTable());
        assertEquals("id", removedFk.getRefColumn());
    }

    @Test
    void diff_shouldHandleNullOldStateAndNullCollectionsGracefully() {
        
        final Map<String, Object> newCreateCtx = new HashMap<>();
        newCreateCtx.put("columns", null);
        newCreateCtx.put("pkColumns", null);
        newCreateCtx.put("fksCtx", null);
        newCreateCtx.put("fks", null);

        final Result result = MigrationDiffer.diff(null, newCreateCtx);

        assertNotNull(result);
        assertTrue(result.getAddedColumns().isEmpty());
        assertTrue(result.getRemovedColumns().isEmpty());
        assertTrue(result.getModifiedColumns().isEmpty());
        assertFalse(result.isPkChanged());
        assertTrue(result.getAddedFks().isEmpty());
        assertTrue(result.getRemovedFks().isEmpty());
    }

    @Test
    void diff_shouldDetectAuditAdded_whenNoPreviousState() {

        final EntityState oldState = null;
        final Map<String, Object> newCreateCtx = new HashMap<>();
        newCreateCtx.put("columns", Collections.emptyList());
        newCreateCtx.put("pkColumns", null);
        newCreateCtx.put("auditEnabled", true);
        newCreateCtx.put("auditCreatedType", "TIMESTAMP");

        final Result result = MigrationDiffer.diff(oldState, newCreateCtx);

        assertTrue(result.isAuditAdded(), "Expected auditAdded=true when audit is newly enabled");
        assertFalse(result.isAuditRemoved(), "auditRemoved should be false");
        assertFalse(result.isAuditTypeChanged(), "auditTypeChanged should be false");
        assertNull(result.getOldAuditType(), "oldAuditType should be null when there was no previous state");
        assertEquals("TIMESTAMP", result.getNewAuditType(), "newAuditType should match auditCreatedType from context");

        assertTrue(result.getAddedColumns().isEmpty());
        assertTrue(result.getRemovedColumns().isEmpty());
        assertTrue(result.getModifiedColumns().isEmpty());
        assertTrue(result.getAddedFks().isEmpty());
        assertTrue(result.getRemovedFks().isEmpty());
    }

    @Test
    void diff_shouldDetectAuditAdded_whenPreviouslyDisabled() {

        final AuditState oldAudit = new AuditState();
        oldAudit.setEnabled(false);
        oldAudit.setType(null);

        final EntityState oldState = new EntityState();
        oldState.setAudit(oldAudit);

        final Map<String, Object> newCreateCtx = new HashMap<>();
        newCreateCtx.put("columns", Collections.emptyList());
        newCreateCtx.put("pkColumns", null);
        newCreateCtx.put("auditEnabled", true);
        newCreateCtx.put("auditCreatedType", "TIMESTAMP");

        final Result result = MigrationDiffer.diff(oldState, newCreateCtx);

        assertTrue(result.isAuditAdded(), "Expected auditAdded=true when audit goes from disabled to enabled");
        assertFalse(result.isAuditRemoved());
        assertFalse(result.isAuditTypeChanged());
        assertNull(result.getOldAuditType());
        assertEquals("TIMESTAMP", result.getNewAuditType());
    }

    @Test
    void diff_shouldDetectAuditRemoved_whenPreviouslyEnabled() {

        final AuditState oldAudit = new AuditState();
        oldAudit.setEnabled(true);
        oldAudit.setType("TIMESTAMP");

        final EntityState oldState = new EntityState();
        oldState.setAudit(oldAudit);

        final Map<String, Object> newCreateCtx = new HashMap<>();
        newCreateCtx.put("columns", Collections.emptyList());
        newCreateCtx.put("pkColumns", null);
        newCreateCtx.put("auditEnabled", false);
        newCreateCtx.put("auditCreatedType", null);

        final Result result = MigrationDiffer.diff(oldState, newCreateCtx);

        assertTrue(result.isAuditRemoved(), "Expected auditRemoved=true when audit goes from enabled to disabled");
        assertFalse(result.isAuditAdded());
        assertFalse(result.isAuditTypeChanged());
        assertEquals("TIMESTAMP", result.getOldAuditType());
        assertNull(result.getNewAuditType());
    }
    
    @Test
        void diff_shouldDetectAuditTypeChanged() {

        final AuditState oldAudit = new AuditState();
        oldAudit.setEnabled(true);
        oldAudit.setType("TIMESTAMP");

        final EntityState oldState = new EntityState();
        oldState.setAudit(oldAudit);

        final Map<String, Object> newCreateCtx = new HashMap<>();
        newCreateCtx.put("columns", Collections.emptyList());
        newCreateCtx.put("pkColumns", null);
        newCreateCtx.put("auditEnabled", true);
        newCreateCtx.put("auditCreatedType", "DATE");

        final Result result = MigrationDiffer.diff(oldState, newCreateCtx);

        assertFalse(result.isAuditAdded());
        assertFalse(result.isAuditRemoved());
        assertTrue(result.isAuditTypeChanged(), "Expected auditTypeChanged=true when type changes but audit remains enabled");
        assertEquals("TIMESTAMP", result.getOldAuditType());
        assertEquals("DATE", result.getNewAuditType());
    }

}
