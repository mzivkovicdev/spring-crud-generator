package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.enums.SwaggerSchemaModeEnum;
import dev.markozivkovic.springcrudgenerator.models.AuditDefinition;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;
import dev.markozivkovic.springcrudgenerator.models.RelationDefinition;
import dev.markozivkovic.springcrudgenerator.models.AuditDefinition.AuditTypeEnum;
import dev.markozivkovic.springcrudgenerator.templates.SwaggerTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.AuditUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.SwaggerUtils;

class SwaggerDocumentationGeneratorTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields, final String description) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        when(m.getDescription()).thenReturn(description);
        when(m.getAudit()).thenReturn(null);
        return m;
    }

    @Test
    void generate_shouldSkipWhenConfigurationIsNull() {
        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final List<ModelDefinition> entities = List.of();

        final SwaggerDocumentationGenerator generator = new SwaggerDocumentationGenerator(null, projectMetadata, entities);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<SwaggerTemplateContext> swaggerCtx = mockStatic(SwaggerTemplateContext.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            generator.generate("out");

            ctx.verifyNoInteractions();
            fieldUtils.verifyNoInteractions();
            writer.verifyNoInteractions();
            tpl.verifyNoInteractions();
            swaggerCtx.verifyNoInteractions();
            swaggerUtils.verifyNoInteractions();
            nameUtils.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenOpenApiNullOrFlagsFalse() {
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getOpenApi()).thenReturn(null);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final List<ModelDefinition> entities = List.of();

        final SwaggerDocumentationGenerator generator = new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class)) {

            generator.generate("out");

            ctx.verifyNoInteractions();
            fieldUtils.verifyNoInteractions();
            writer.verifyNoInteractions();
            tpl.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenApiSpecOrGenerateResourcesFalse() {
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.OpenApiDefinition openApi = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApi);
        when(openApi.getApiSpec()).thenReturn(false);
        when(openApi.getGenerateResources()).thenReturn(false);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final List<ModelDefinition> entities = List.of();

        final SwaggerDocumentationGenerator generator = new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class)) {

            generator.generate("out");

            ctx.verifyNoInteractions();
            fieldUtils.verifyNoInteractions();
            writer.verifyNoInteractions();
            tpl.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldSkipWhenAlreadyGenerated() {
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.OpenApiDefinition openApi = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApi);
        when(openApi.getApiSpec()).thenReturn(true);
        when(openApi.getGenerateResources()).thenReturn(true);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final List<ModelDefinition> entities = List.of();

        final SwaggerDocumentationGenerator generator = new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER)).thenReturn(true);

            generator.generate("out");

            fieldUtils.verifyNoInteractions();
            writer.verifyNoInteractions();
            tpl.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateSwaggerDocsForEntitiesWithIdAndRelations() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.OpenApiDefinition openApi = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApi);
        when(openApi.getApiSpec()).thenReturn(true);
        when(openApi.getGenerateResources()).thenReturn(true);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final FieldDefinition userIdField = mock(FieldDefinition.class);
        final FieldDefinition userRelationField = mock(FieldDefinition.class);
        final FieldDefinition addrIdField = mock(FieldDefinition.class);

        final RelationDefinition relation = mock(RelationDefinition.class);
        when(userRelationField.getRelation()).thenReturn(relation);
        when(userRelationField.getType()).thenReturn("AddressEntity");

        final ModelDefinition userEntity = newModel("UserEntity", List.of(userIdField, userRelationField), null);
        final ModelDefinition addressEntity = newModel("AddressEntity", List.of(addrIdField), null);

        final List<ModelDefinition> entities = List.of(userEntity, addressEntity);

        final SwaggerDocumentationGenerator generator = new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

        final List<String> writtenFiles = new ArrayList<>();
        final List<String> swaggerTemplateCalls = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<SwaggerTemplateContext> swaggerCtx = mockStatic(SwaggerTemplateContext.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(userEntity.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(addressEntity.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(userEntity.getFields())).thenReturn(userIdField);
            fieldUtils.when(() -> FieldUtils.extractIdField(addressEntity.getFields())).thenReturn(addrIdField);
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of());
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class))).thenReturn(new HashMap<>());

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity")).thenReturn("Address");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("Address")).thenReturn("Address");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiCreateModelName("User")).thenReturn("UserCreate");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("User")).thenReturn("UserUpdate");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiCreateModelName("Address")).thenReturn("AddressCreate");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("Address")).thenReturn("AddressUpdate");

            swaggerCtx.when(() -> SwaggerTemplateContext.computeSwaggerTemplateContext(any(ModelDefinition.class))).thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeRelationEndpointContext(any(ModelDefinition.class), eq(entities)))
                    .thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeContextWithId(any(ModelDefinition.class)))
                    .thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeBaseContext(any(ModelDefinition.class)))
                    .thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/schema/object-template.ftl"), anyMap())).thenReturn("OBJECT_SCHEMA");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/endpoint/create-endpoint.ftl"), anyMap())).thenReturn("CREATE_EP");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/endpoint/get-all-endpoint.ftl"), anyMap())).thenReturn("GET_ALL_EP");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/endpoint/get-by-id-endpoint.ftl"), anyMap())).thenReturn("GET_BY_ID_EP");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/endpoint/delete-by-id-endpoint.ftl"), anyMap())).thenReturn("DELETE_BY_ID_EP");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/endpoint/update-by-id-endpoint.ftl"), anyMap())).thenReturn("UPDATE_BY_ID_EP");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/endpoint/relation-endpoint.ftl"), anyMap())).thenReturn("REL_EP");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/swagger-template.ftl"), anyMap()))
                .thenAnswer(inv -> {
                    swaggerTemplateCalls.add("CALLED");
                    return "SWAGGER_DOC";
                });

            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString(), anyString()))
                    .thenAnswer(inv -> {
                        writtenFiles.add(inv.getArgument(2, String.class));
                        return null;
                    });

            generator.generate("out");

            assertTrue(writtenFiles.contains("user-api.yaml"), "Expected user-api.yaml to be generated");
            assertTrue(writtenFiles.contains("address-api.yaml"), "Expected address-api.yaml to be generated");
            assertTrue(writtenFiles.contains("user.yaml"), "Expected user.yaml object schema");
            assertTrue(writtenFiles.contains("address.yaml"), "Expected address.yaml object schema");
            assertTrue(writtenFiles.contains("addressInput.yaml"), "Expected addressInput.yaml relation input schema");
            assertTrue(writtenFiles.contains("userCreate.yaml"), "Expected userCreate.yaml create schema");
            assertTrue(writtenFiles.contains("userUpdate.yaml"), "Expected userUpdate.yaml update schema");
            assertTrue(writtenFiles.contains("addressCreate.yaml"), "Expected addressCreate.yaml create schema");
            assertTrue(writtenFiles.contains("addressUpdate.yaml"), "Expected addressUpdate.yaml update schema");
            assertEquals(2, swaggerTemplateCalls.size());

            ctx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER));
        }
    }

    @Test
    void generate_shouldThrowWhenRelationModelNotFound() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.OpenApiDefinition openApi = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApi);
        when(openApi.getApiSpec()).thenReturn(true);
        when(openApi.getGenerateResources()).thenReturn(true);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final FieldDefinition userIdField = mock(FieldDefinition.class);
        final FieldDefinition userRelationField = mock(FieldDefinition.class);

        final RelationDefinition relation = mock(RelationDefinition.class);
        when(userRelationField.getRelation()).thenReturn(relation);
        when(userRelationField.getType()).thenReturn("AddressEntity");

        final ModelDefinition userEntity = newModel("UserEntity", List.of(userIdField, userRelationField), null);
        final List<ModelDefinition> entities = List.of(userEntity);

        final SwaggerDocumentationGenerator generator = new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(userEntity.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRequiredFields(anyList())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRequiredFieldsForCreate(anyList())).thenReturn(List.of());
            fieldUtils.when(() -> FieldUtils.extractRequiredFieldsForUpdate(anyList())).thenReturn(List.of());
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class))).thenReturn(new HashMap<>());
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class), eq(SwaggerSchemaModeEnum.INPUT))).thenReturn(new HashMap<>());
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiCreateModelName("User")).thenReturn("UserCreate");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("User")).thenReturn("UserUpdate");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/schema/object-template.ftl"), anyMap())).thenReturn("OBJECT_SCHEMA");

            assertThrows(IllegalArgumentException.class, () -> generator.generate("out"));

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/swagger-template.ftl"), anyMap()), never());
            ctx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER), never());
        }
    }

    @Test
    void generateObjects_shouldIncludeAuditFieldsWhenAuditEnabled_onlyOnDefaultSchema() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.OpenApiDefinition openApi = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApi);
        when(openApi.getApiSpec()).thenReturn(true);
        when(openApi.getGenerateResources()).thenReturn(true);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final FieldDefinition idField = mock(FieldDefinition.class);
        final AuditDefinition audit = mock(AuditDefinition.class);
        when(audit.getEnabled()).thenReturn(true);
        when(audit.getType()).thenReturn(AuditTypeEnum.INSTANT);

        final ModelDefinition entity = newModel("ProductEntity", List.of(idField), "desc");
        when(entity.getAudit()).thenReturn(audit);

        final SwaggerDocumentationGenerator generator =
                new SwaggerDocumentationGenerator(cfg, projectMetadata, List.of(entity));

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
            final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
            final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
            final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class);
            final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class);
            final MockedStatic<SwaggerTemplateContext> swaggerCtx = mockStatic(SwaggerTemplateContext.class);
            final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
            final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(entity.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(entity.getFields())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of());
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class))).thenReturn(new HashMap<>());
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class), eq(SwaggerSchemaModeEnum.INPUT))).thenReturn(new HashMap<>());
            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProductEntity")).thenReturn("Product");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("Product")).thenReturn("ProductDTO");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiCreateModelName("Product")).thenReturn("ProductCreateDTO");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("Product")).thenReturn("ProductUpdateDTO");
            auditUtils.when(() -> AuditUtils.resolveAuditType(AuditTypeEnum.INSTANT)).thenReturn("INSTANT");
            swaggerUtils.when(() -> SwaggerUtils.resolve(eq("INSTANT"), eq(List.of()))).thenReturn(Map.of("type", "string", "format", "date-time"));
            swaggerCtx.when(() -> SwaggerTemplateContext.computeSwaggerTemplateContext(any(ModelDefinition.class))).thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeRelationEndpointContext(any(ModelDefinition.class), anyList())).thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeContextWithId(any(ModelDefinition.class))).thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeBaseContext(any(ModelDefinition.class))).thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/schema/object-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> modelCtx = inv.getArgument(1, Map.class);
                        final String title = (String) modelCtx.get("title");
                        assertNotNull(title);
                        if ("ProductDTO".equals(title)) {
                            assertEquals(true, modelCtx.get(TemplateContextConstants.AUDIT_ENABLED));
                            assertNotNull(modelCtx.get(TemplateContextConstants.AUDIT_TYPE));
                        } else {
                            assertFalse(Boolean.TRUE.equals(modelCtx.get(TemplateContextConstants.AUDIT_ENABLED)));
                            assertNull(modelCtx.get(TemplateContextConstants.AUDIT_TYPE));
                        }
                        return "OBJECT_SCHEMA";
                    });

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/swagger-template.ftl"), anyMap())).thenReturn("SWAGGER_DOC");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(startsWith("swagger/endpoint/"), anyMap())).thenReturn("EP");

            generator.generate("out");

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/schema/object-template.ftl"), anyMap()), times(3));
        }
    }

    @Test
    void generate_shouldGenerateJsonObjectSchemasWhenJsonModelsPresent() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.OpenApiDefinition openApi = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApi);
        when(openApi.getApiSpec()).thenReturn(true);
        when(openApi.getGenerateResources()).thenReturn(true);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final FieldDefinition idA = mock(FieldDefinition.class);
        final FieldDefinition jsonField = mock(FieldDefinition.class);

        final ModelDefinition entityA = newModel("AEntity", List.of(idA, jsonField), null);

        final FieldDefinition jsonId = mock(FieldDefinition.class);
        final ModelDefinition jsonModel = newModel("JsonModel", List.of(jsonId), null);

        final List<ModelDefinition> entities = List.of(entityA, jsonModel);

        final SwaggerDocumentationGenerator generator = new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

        final List<String> writtenFiles = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(entityA.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(jsonModel.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(entityA.getFields())).thenReturn(idA);
            fieldUtils.when(() -> FieldUtils.extractIdField(jsonModel.getFields())).thenReturn(jsonId);
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of(jsonField));
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField)).thenReturn("JsonModel");
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class))).thenReturn(new HashMap<>());
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AEntity")).thenReturn("A");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("A")).thenReturn("A");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiCreateModelName("A")).thenReturn("ACreate");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("A")).thenReturn("AUpdate");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiCreateModelName("JsonModel")).thenReturn("JsonModelCreate");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("JsonModel")).thenReturn("JsonModelUpdate");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("JsonModel")).thenReturn("JsonModel");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("JsonModel")).thenReturn("JsonModel");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/schema/object-template.ftl"), anyMap())).thenReturn("OBJECT_SCHEMA");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/swagger-template.ftl"), anyMap())).thenReturn("SWAGGER_DOC");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("X");

            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString(), anyString()))
                    .thenAnswer(inv -> {
                        writtenFiles.add(inv.getArgument(2, String.class));
                        return null;
                    });

            generator.generate("out");
        }

        assertTrue(writtenFiles.contains("jsonModel.yaml") || writtenFiles.contains("jsonmodel.yaml"),
                "Expected JSON model schema file to be generated (name depends on your uncapitalize logic)");
    }
    @Test
    @DisplayName("generateObjects with CREATE_MODEL mode should exclude ID field and use INPUT mode")
    void generateObjects_createMode_shouldExcludeIdAndUseInputMode() {
        
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.OpenApiDefinition openApi = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApi);
        when(openApi.getApiSpec()).thenReturn(true);
        when(openApi.getGenerateResources()).thenReturn(true);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final FieldDefinition idField = mock(FieldDefinition.class);
        final FieldDefinition nameField = mock(FieldDefinition.class);

        final ModelDefinition entity = newModel("UserEntity", List.of(idField, nameField), null);

        final SwaggerDocumentationGenerator generator = new SwaggerDocumentationGenerator(cfg, projectMetadata, List.of(entity));

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
            final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
            final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
            final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class);
            final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
            final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(entity.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(entity.getFields())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of());
            
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(eq(nameField), any())).thenReturn(Map.of("name", "name"));
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class))).thenReturn(new HashMap<>());
            
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiCreateModelName("User")).thenReturn("UserCreate");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("User")).thenReturn("UserUpdate");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/schema/object-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> modelCtx = inv.getArgument(1, Map.class);
                        
                        if ("UserCreate".equals(modelCtx.get("title"))) {
                            @SuppressWarnings("unchecked")
                            final List<Map<String, Object>> props = (List<Map<String, Object>>) modelCtx.get("properties");
                            assertEquals(1, props.size(), "CREATE mode should exclude ID field");
                        }
                        return "OBJECT_SCHEMA";
                    });

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/swagger-template.ftl"), anyMap())).thenReturn("SWAGGER_DOC");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("X");

            generator.generate("out");

            swaggerUtils.verify(() -> SwaggerUtils.toSwaggerProperty(eq(nameField), any()));
        }
    }

    @Test
    @DisplayName("generate should create schema names list with all model variations")
    void generate_shouldCreateSchemaNamesList() {
        
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.OpenApiDefinition openApi = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApi);
        when(openApi.getApiSpec()).thenReturn(true);
        when(openApi.getGenerateResources()).thenReturn(true);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final FieldDefinition idField = mock(FieldDefinition.class);
        final FieldDefinition relationField = mock(FieldDefinition.class);
        final RelationDefinition relation = mock(RelationDefinition.class);
        when(relationField.getRelation()).thenReturn(relation);
        when(relationField.getType()).thenReturn("AddressEntity");

        final FieldDefinition addrId = mock(FieldDefinition.class);

        final ModelDefinition userEntity = newModel("UserEntity", List.of(idField, relationField), null);
        final ModelDefinition addressEntity = newModel("AddressEntity", List.of(addrId), null);

        final List<ModelDefinition> entities = List.of(userEntity, addressEntity);

        final SwaggerDocumentationGenerator generator = new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
            final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
            final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
            final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class);
            final MockedStatic<SwaggerTemplateContext> swaggerCtx = mockStatic(SwaggerTemplateContext.class);
            final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
            final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER)).thenReturn(false);
            
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(userEntity.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(addressEntity.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(userEntity.getFields())).thenReturn(idField);
            fieldUtils.when(() -> FieldUtils.extractIdField(addressEntity.getFields())).thenReturn(addrId);
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of());
            
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class))).thenReturn(new HashMap<>());
            
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity")).thenReturn("Address");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiCreateModelName("User")).thenReturn("UserCreate");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("User")).thenReturn("UserUpdate");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("Address")).thenReturn("Address");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiCreateModelName("Address")).thenReturn("AddressCreate");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiUpdateModelName("Address")).thenReturn("AddressUpdate");

            swaggerCtx.when(() -> SwaggerTemplateContext.computeSwaggerTemplateContext(any(ModelDefinition.class))).thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeRelationEndpointContext(any(ModelDefinition.class), eq(entities)))
                    .thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeContextWithId(any(ModelDefinition.class)))
                    .thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeBaseContext(any(ModelDefinition.class)))
                    .thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/schema/object-template.ftl"), anyMap())).thenReturn("OBJECT_SCHEMA");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/swagger-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> context = inv.getArgument(1, Map.class);
                        
                        @SuppressWarnings("unchecked")
                        final List<String> schemaNames = (List<String>) context.get("schemaNames");
                        
                        assertNotNull(schemaNames);
                        assertTrue(schemaNames.contains("user"), "Should contain main model name");
                        assertTrue(schemaNames.contains("userCreate"), "Should contain create model name");
                        assertTrue(schemaNames.contains("userUpdate"), "Should contain update model name");
                        assertTrue(schemaNames.contains("addressInput"), "Should contain relation input model name");
                        
                        return "SWAGGER_DOC";
                    });
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(anyString(), anyMap())).thenReturn("X");

            generator.generate("out");
        }
    }

    @Test
    @DisplayName("generate should set relation input schema property name to relation model id field name")
    void generate_shouldUseRelationIdFieldNameInRelationInputSchema() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.OpenApiDefinition openApi = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApi);
        when(openApi.getApiSpec()).thenReturn(true);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final FieldDefinition userIdField = mock(FieldDefinition.class);

        final FieldDefinition userRelationField = mock(FieldDefinition.class);
        final RelationDefinition relation = mock(RelationDefinition.class);
        when(userRelationField.getRelation()).thenReturn(relation);
        when(userRelationField.getType()).thenReturn("AddressEntity");

        final ModelDefinition userEntity = newModel("UserEntity", List.of(userIdField, userRelationField), null);

        final FieldDefinition addressIdField = mock(FieldDefinition.class);
        when(addressIdField.getName()).thenReturn("addressId");
        final ModelDefinition addressEntity = newModel("AddressEntity", List.of(addressIdField), null);

        final List<ModelDefinition> entities = List.of(userEntity, addressEntity);

        final SwaggerDocumentationGenerator generator = new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class);
             final MockedStatic<SwaggerTemplateContext> swaggerCtx = mockStatic(SwaggerTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER)).thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(userEntity.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(addressEntity.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(userEntity.getFields())).thenReturn(userIdField);
            fieldUtils.when(() -> FieldUtils.extractIdField(addressEntity.getFields())).thenReturn(addressIdField);
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of());
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class))).thenReturn(new HashMap<>());
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class), eq(SwaggerSchemaModeEnum.INPUT))).thenReturn(new HashMap<>());

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity")).thenReturn("Address");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName(anyString())).thenAnswer(inv -> inv.getArgument(0));
            nameUtils.when(() -> ModelNameUtils.computeOpenApiCreateModelName(anyString())).thenAnswer(inv -> inv.getArgument(0) + "Create");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiUpdateModelName(anyString())).thenAnswer(inv -> inv.getArgument(0) + "Update");

            swaggerCtx.when(() -> SwaggerTemplateContext.computeSwaggerTemplateContext(any(ModelDefinition.class))).thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeRelationEndpointContext(any(ModelDefinition.class), eq(entities))).thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeContextWithId(any(ModelDefinition.class))).thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeBaseContext(any(ModelDefinition.class))).thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/schema/object-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> modelCtx = inv.getArgument(1, Map.class);
                        if ("AddressInput".equals(modelCtx.get("title"))) {
                            @SuppressWarnings("unchecked")
                            final List<Map<String, Object>> props = (List<Map<String, Object>>) modelCtx.get("properties");
                            assertEquals(1, props.size());
                            final Map<String, Object> idProp = props.get(0);
                            assertEquals("addressId", idProp.get("name"), "Expected relation input property name to match id field name");
                        }
                        return "OBJECT_SCHEMA";
                    });

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/swagger-template.ftl"), anyMap())).thenReturn("SWAGGER_DOC");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(startsWith("swagger/endpoint/"), anyMap())).thenReturn("EP");
            generator.generate("out");
        }
    }

    @Test
    @DisplayName("generate should skip entities without ID field")
    void generate_shouldSkipEntitiesWithoutId() {
        
        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final CrudConfiguration.OpenApiDefinition openApi = mock(CrudConfiguration.OpenApiDefinition.class);
        when(cfg.getOpenApi()).thenReturn(openApi);
        when(openApi.getApiSpec()).thenReturn(true);
        when(openApi.getGenerateResources()).thenReturn(true);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final FieldDefinition nameField = mock(FieldDefinition.class);
        final ModelDefinition entityWithoutId = newModel("NoIdEntity", List.of(nameField), null);

        final List<ModelDefinition> entities = List.of(entityWithoutId);

        final SwaggerDocumentationGenerator generator = new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

        final List<String> writtenFiles = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
            final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
            final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(entityWithoutId.getFields())).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of());

            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString(), anyString()))
                    .thenAnswer(inv -> {
                        writtenFiles.add(inv.getArgument(2, String.class));
                        return null;
                    });

            generator.generate("out");

            assertTrue(writtenFiles.isEmpty(), "Should not generate any files for entities without ID");
            
            ctx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER));
        }
    }
}