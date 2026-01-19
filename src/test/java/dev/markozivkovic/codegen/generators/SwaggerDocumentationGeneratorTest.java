package dev.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.constants.GeneratorConstants;
import dev.markozivkovic.codegen.constants.TemplateContextConstants;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.models.AuditDefinition;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.ProjectMetadata;
import dev.markozivkovic.codegen.models.RelationDefinition;
import dev.markozivkovic.codegen.models.AuditDefinition.AuditTypeEnum;
import dev.markozivkovic.codegen.templates.SwaggerTemplateContext;
import dev.markozivkovic.codegen.utils.AuditUtils;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.SwaggerUtils;

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
        when(openApi.getApiSpec()).thenReturn(true);
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
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class))).thenReturn(new HashMap<>());
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("User")).thenReturn("User");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/schema/object-template.ftl"), anyMap())).thenReturn("OBJECT_SCHEMA");

            assertThrows(IllegalArgumentException.class, () -> generator.generate("out"));

            tpl.verify(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/swagger-template.ftl"), anyMap()), never());
            ctx.verify(() -> GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER), never());
        }
    }

    @Test
    void generateObjects_shouldIncludeAuditFieldsWhenAuditEnabled() {

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

        final SwaggerDocumentationGenerator generator = new SwaggerDocumentationGenerator(cfg, projectMetadata, List.of(entity));

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class);
             final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(entity.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(entity.getFields())).thenReturn(idField);
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class))).thenReturn(new HashMap<>());
            nameUtils.when(() -> ModelNameUtils.stripSuffix("ProductEntity")).thenReturn("Product");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("Product")).thenReturn("Product");

            auditUtils.when(() -> AuditUtils.resolveAuditType(AuditTypeEnum.INSTANT)).thenReturn("INSTANT");
            swaggerUtils.when(() -> SwaggerUtils.resolve(eq("INSTANT"), eq(List.of()))).thenReturn(Map.of("type", "string", "format", "date-time"));

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/schema/object-template.ftl"), anyMap()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> modelCtx = inv.getArgument(1, Map.class);
                        assertEquals(true, modelCtx.get(TemplateContextConstants.AUDIT_ENABLED));
                        assertNotNull(modelCtx.get(TemplateContextConstants.AUDIT_TYPE));
                        return "OBJECT_SCHEMA";
                    });

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/swagger-template.ftl"), anyMap())).thenReturn("SWAGGER_DOC");

            generator.generate("out");
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

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER))
               .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(entityA.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(jsonModel.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractIdField(entityA.getFields())).thenReturn(idA);
            fieldUtils.when(() -> FieldUtils.extractIdField(jsonModel.getFields())).thenReturn(jsonId);
            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList())).thenReturn(List.of(jsonField));
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField)).thenReturn("JsonModel");
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(any(FieldDefinition.class))).thenReturn(new HashMap<>());
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AEntity")).thenReturn("A");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("A")).thenReturn("A");
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
}