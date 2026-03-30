package dev.markozivkovic.springcrudgenerator.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.imports.RepositoryImports;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;

class JpaRepositoryTemplateContextTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @Test
    void computeJpaInterfaceContext_shouldSetCoreFields_andImports_andEntityGraph() {

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("java.util.UUID");
        when(idField.getName()).thenReturn("id");

        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("UserEntity", fields);

        final boolean openInViewEnabled = true;
        final String packagePath = "com.example.app";
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final List<String> lazyNames = List.of("tags");
        final String graphName = "User.withTags";
        final String baseImports = "import java.util.UUID;\n";
        final String projectImports = "import com.example.app.entity.UserEntity;\n";

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<RepositoryImports> repoImports = mockStatic(RepositoryImports.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(lazyNames);
            fieldUtils.when(() -> FieldUtils.hasLazyFetchField(fields)).thenReturn(true);
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeEntityGraphName("UserEntity", lazyNames)).thenReturn(graphName);
            repoImports.when(() -> RepositoryImports.computeJpaRepostiroyImports(model, openInViewEnabled)).thenReturn(baseImports);
            repoImports.when(() -> RepositoryImports.computeProjectImports(packagePath, pkgCfg, "UserEntity")).thenReturn(projectImports);

            final Map<String, Object> ctx = JpaRepositoryTemplateContext.computeJpaInterfaceContext(
                    model, openInViewEnabled, packagePath, pkgCfg
            );

            assertEquals("UserRepository", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals("UserEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("java.util.UUID", ctx.get(TemplateContextConstants.ID_TYPE));
            assertEquals("id", ctx.get(TemplateContextConstants.ID_FIELD));
            assertEquals(graphName, ctx.get(TemplateContextConstants.ENTITY_GRAPH_NAME));
            assertEquals(openInViewEnabled, ctx.get(TemplateContextConstants.OPEN_IN_VIEW_ENABLED));
            assertEquals(baseImports, ctx.get(TemplateContextConstants.BASE_IMPORTS));
            assertEquals(projectImports, ctx.get(TemplateContextConstants.PROJECT_IMPORTS));
            assertEquals(true, ctx.get(TemplateContextConstants.HAS_LAZY_FIELDS));

            fieldUtils.verify(() -> FieldUtils.extractIdField(fields));
            fieldUtils.verify(() -> FieldUtils.extractLazyFetchFieldNames(fields));
            nameUtils.verify(() -> ModelNameUtils.stripSuffix("UserEntity"));
            nameUtils.verify(() -> ModelNameUtils.computeEntityGraphName("UserEntity", lazyNames));
            repoImports.verify(() -> RepositoryImports.computeJpaRepostiroyImports(model, openInViewEnabled));
            repoImports.verify(() -> RepositoryImports.computeProjectImports(packagePath, pkgCfg, "UserEntity"));
        }
    }

    @Test
    void computeJpaInterfaceContext_shouldWorkWithDifferentModelAndIdType() {

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");
        when(idField.getName()).thenReturn("orderId");

        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("OrderEntity", fields);

        final boolean openInViewEnabled = true;
        final String packagePath = "com.example.app";
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final List<String> lazyNames = List.of();
        final String graphName = "Order.with";
        final String baseImports = "";
        final String projectImports = "import com.example.app.entity.OrderEntity;\n";

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<RepositoryImports> repoImports = mockStatic(RepositoryImports.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(lazyNames);
            fieldUtils.when(() -> FieldUtils.hasLazyFetchField(fields)).thenReturn(false);
            nameUtils.when(() -> ModelNameUtils.stripSuffix("OrderEntity")).thenReturn("Order");
            nameUtils.when(() -> ModelNameUtils.computeEntityGraphName("OrderEntity", lazyNames)).thenReturn(graphName);
            repoImports.when(() -> RepositoryImports.computeJpaRepostiroyImports(model, openInViewEnabled)).thenReturn(baseImports);
            repoImports.when(() -> RepositoryImports.computeProjectImports(packagePath, pkgCfg, "OrderEntity")).thenReturn(projectImports);

            final Map<String, Object> ctx = JpaRepositoryTemplateContext.computeJpaInterfaceContext(
                    model, openInViewEnabled, packagePath, pkgCfg
            );

            assertEquals("OrderRepository", ctx.get(TemplateContextConstants.CLASS_NAME));
            assertEquals("OrderEntity", ctx.get(TemplateContextConstants.MODEL_NAME));
            assertEquals("Long", ctx.get(TemplateContextConstants.ID_TYPE));
            assertEquals("orderId", ctx.get(TemplateContextConstants.ID_FIELD));
            assertEquals(graphName, ctx.get(TemplateContextConstants.ENTITY_GRAPH_NAME));
            assertEquals(false, ctx.get(TemplateContextConstants.HAS_LAZY_FIELDS));
        }
    }

    @Test
    void computeJpaInterfaceContext_shouldPropagateOpenInViewFalse_andStillSetHasLazyFields() {

        final FieldDefinition idField = mock(FieldDefinition.class);
        when(idField.getType()).thenReturn("Long");
        when(idField.getName()).thenReturn("id");

        final List<FieldDefinition> fields = List.of(idField);
        final ModelDefinition model = newModel("UserEntity", fields);

        final boolean openInViewEnabled = false;
        final String packagePath = "com.example.app";
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final List<String> lazyNames = List.of("tags", "orders");
        final String graphName = "User.withTagsOrders";

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<RepositoryImports> repoImports = mockStatic(RepositoryImports.class)) {

            fieldUtils.when(() -> FieldUtils.extractIdField(fields)).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractLazyFetchFieldNames(fields)).thenReturn(lazyNames);
            fieldUtils.when(() -> FieldUtils.hasLazyFetchField(fields)).thenReturn(true);
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeEntityGraphName("UserEntity", lazyNames)).thenReturn(graphName);

            repoImports.when(() -> RepositoryImports.computeJpaRepostiroyImports(model, openInViewEnabled))
                       .thenReturn("import java.util.Optional;\n");
            repoImports.when(() -> RepositoryImports.computeProjectImports(packagePath, pkgCfg, "UserEntity"))
                       .thenReturn("import com.example.app.entity.UserEntity;\n");

            final Map<String, Object> ctx = JpaRepositoryTemplateContext.computeJpaInterfaceContext(
                    model, openInViewEnabled, packagePath, pkgCfg
            );

            assertEquals(false, ctx.get(TemplateContextConstants.OPEN_IN_VIEW_ENABLED));
            assertEquals(true, ctx.get(TemplateContextConstants.HAS_LAZY_FIELDS));
            assertEquals(graphName, ctx.get(TemplateContextConstants.ENTITY_GRAPH_NAME));
            assertTrue(((String) ctx.get(TemplateContextConstants.BASE_IMPORTS)).contains("java.util.Optional"));
        }
    }
}
