package com.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.ProjectMetadata;
import com.markozivkovic.codegen.models.RelationDefinition;
import com.markozivkovic.codegen.templates.SwaggerTemplateContext;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.SwaggerUtils;

class SwaggerDocumentationGeneratorTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields, final String description) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        when(m.getDescription()).thenReturn(description);
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
    void generate_shouldSkipWhenSwaggerFlagIsNullOrFalse() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getOpenApi()).thenReturn(null);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final List<ModelDefinition> entities = List.of();

        final SwaggerDocumentationGenerator generator =
                new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

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
    void generate_shouldSkipWhenAlreadyGenerated() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getOpenApi()).thenReturn(mock(CrudConfiguration.OpenApiDefinition.class));
        when(cfg.getOpenApi().getApiSpec()).thenReturn(true);
        when(cfg.getOpenApi().getGenerateResources()).thenReturn(true);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        final List<ModelDefinition> entities = List.of();

        final SwaggerDocumentationGenerator generator =
                new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(
                    GeneratorConstants.GeneratorContextKeys.SWAGGER))
               .thenReturn(true);

            generator.generate("out");

            fieldUtils.verifyNoInteractions();
            writer.verifyNoInteractions();
            tpl.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateSwaggerDocsForEntitiesWithIdAndRelations() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getOpenApi()).thenReturn(mock(CrudConfiguration.OpenApiDefinition.class));
        when(cfg.getOpenApi().getApiSpec()).thenReturn(true);
        when(cfg.getOpenApi().getGenerateResources()).thenReturn(true);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final FieldDefinition userIdField = mock(FieldDefinition.class);
        final FieldDefinition userRelationField = mock(FieldDefinition.class);
        final FieldDefinition addrIdField = mock(FieldDefinition.class);

        final RelationDefinition relation = mock(RelationDefinition.class);
        when(relation.getType()).thenReturn("AddressEntity");
        when(userRelationField.getRelation()).thenReturn(relation);
        when(userRelationField.getType()).thenReturn("AddressEntity");

        final ModelDefinition userEntity = newModel("UserEntity", List.of(userIdField, userRelationField), null);
        final ModelDefinition addressEntity = newModel("AddressEntity", List.of(addrIdField), null);

        final List<ModelDefinition> entities = List.of(userEntity, addressEntity);

        final SwaggerDocumentationGenerator generator =
                new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

        final List<String> writtenFiles = new ArrayList<>();
        final List<String> swaggerTemplateCalls = new ArrayList<>();

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<SwaggerTemplateContext> swaggerCtx = mockStatic(SwaggerTemplateContext.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(
                    GeneratorConstants.GeneratorContextKeys.SWAGGER))
               .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(userEntity.getFields()))
                      .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(addressEntity.getFields()))
                      .thenReturn(true);

            fieldUtils.when(() -> FieldUtils.extractRelationFields(userEntity.getFields()))
                      .thenReturn(List.of(userRelationField));
            fieldUtils.when(() -> FieldUtils.extractRelationFields(addressEntity.getFields()))
                      .thenReturn(List.of());

            fieldUtils.when(() -> FieldUtils.extractIdField(userEntity.getFields()))
                      .thenReturn(userIdField);
            fieldUtils.when(() -> FieldUtils.extractIdField(addressEntity.getFields()))
                      .thenReturn(addrIdField);

            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList()))
                      .thenReturn(List.of());

            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(userIdField))
                        .thenReturn(new HashMap<>());
            swaggerUtils.when(() -> SwaggerUtils.toSwaggerProperty(addrIdField))
                        .thenReturn(new HashMap<>());

            swaggerCtx.when(() -> SwaggerTemplateContext.computeSwaggerTemplateContext(userEntity))
                      .thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeSwaggerTemplateContext(addressEntity))
                      .thenReturn(new HashMap<>());

            swaggerCtx.when(() -> SwaggerTemplateContext.computeRelationEndpointContext(userEntity, entities))
                      .thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeRelationEndpointContext(addressEntity, entities))
                      .thenReturn(new HashMap<>());

            swaggerCtx.when(() -> SwaggerTemplateContext.computeContextWithId(userEntity))
                      .thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeContextWithId(addressEntity))
                      .thenReturn(new HashMap<>());

            swaggerCtx.when(() -> SwaggerTemplateContext.computeBaseContext(userEntity))
                      .thenReturn(new HashMap<>());
            swaggerCtx.when(() -> SwaggerTemplateContext.computeBaseContext(addressEntity))
                      .thenReturn(new HashMap<>());

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity"))
                     .thenReturn("Address");

            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("User"))
                     .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("Address"))
                     .thenReturn("Address");

            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("UserEntity"))
                     .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("AddressEntity"))
                     .thenReturn("Address");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("swagger/schema/object-template.ftl"), anyMap()))
               .thenReturn("OBJECT_SCHEMA");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("swagger/endpoint/create-endpoint.ftl"), anyMap()))
               .thenReturn("CREATE_EP");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("swagger/endpoint/get-all-endpoint.ftl"), anyMap()))
               .thenReturn("GET_ALL_EP");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("swagger/endpoint/get-by-id-endpoint.ftl"), anyMap()))
               .thenReturn("GET_BY_ID_EP");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("swagger/endpoint/delete-by-id-endpoint.ftl"), anyMap()))
               .thenReturn("DELETE_BY_ID_EP");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("swagger/endpoint/update-by-id-endpoint.ftl"), anyMap()))
               .thenReturn("UPDATE_BY_ID_EP");
            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("swagger/endpoint/relation-endpoint.ftl"), anyMap()))
               .thenReturn("REL_EP");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(
                    eq("swagger/swagger-template.ftl"), anyMap()))
               .thenAnswer(inv -> {
                   swaggerTemplateCalls.add("CALLED");
                   return "SWAGGER_DOC";
               });

            writer.when(() -> FileWriterUtils.writeToFile(anyString(), anyString(), anyString(), anyString()))
                  .thenAnswer(inv -> {
                      final String name = inv.getArgument(2, String.class);
                      writtenFiles.add(name);
                      return null;
                  });

            generator.generate("out");

            assertTrue(writtenFiles.contains("user-api.yaml"), "Expected user-api.yaml to be generated");
            assertTrue(writtenFiles.contains("address-api.yaml"), "Expected address-api.yaml to be generated");
            assertTrue(writtenFiles.contains("user.yaml"), "Expected user.yaml object schema");
            assertTrue(writtenFiles.contains("address.yaml"), "Expected address.yaml object schema");
            assertTrue(writtenFiles.contains("addressInput.yaml"), "Expected addressInput.yaml relation input schema");

            assertEquals(2, swaggerTemplateCalls.size());

            ctx.verify(() -> GeneratorContext.markGenerated(
                    GeneratorConstants.GeneratorContextKeys.SWAGGER));
        }
    }

    @Test
    void generate_shouldThrowWhenRelationModelNotFound() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        when(cfg.getOpenApi()).thenReturn(mock(CrudConfiguration.OpenApiDefinition.class));
        when(cfg.getOpenApi().getApiSpec()).thenReturn(true);
        when(cfg.getOpenApi().getGenerateResources()).thenReturn(true);

        final ProjectMetadata projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getProjectBaseDir()).thenReturn("/tmp/project");

        final FieldDefinition userIdField = mock(FieldDefinition.class);
        final FieldDefinition userRelationField = mock(FieldDefinition.class);

        final RelationDefinition relation = mock(RelationDefinition.class);
        when(relation.getType()).thenReturn("AddressEntity");
        when(userRelationField.getRelation()).thenReturn(relation);
        when(userRelationField.getType()).thenReturn("AddressEntity");

        final ModelDefinition userEntity = newModel("UserEntity", List.of(userIdField, userRelationField), null);
        final List<ModelDefinition> entities = List.of(userEntity);

        final SwaggerDocumentationGenerator generator =
                new SwaggerDocumentationGenerator(cfg, projectMetadata, entities);

        try (final MockedStatic<GeneratorContext> ctx = mockStatic(GeneratorContext.class);
             final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<SwaggerTemplateContext> swaggerCtx = mockStatic(SwaggerTemplateContext.class);
             final MockedStatic<SwaggerUtils> swaggerUtils = mockStatic(SwaggerUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class)) {

            ctx.when(() -> GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.SWAGGER))
                    .thenReturn(false);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(userEntity.getFields()))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractRelationFields(userEntity.getFields()))
                    .thenReturn(List.of(userRelationField));

            fieldUtils.when(() -> FieldUtils.extractJsonFields(anyList()))
                    .thenReturn(List.of());

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                    .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.computeOpenApiModelName("User"))
                    .thenReturn("User");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("swagger/schema/object-template.ftl"), anyMap()))
                    .thenReturn("OBJECT_SCHEMA");

            assertThrows(IllegalArgumentException.class,
                    () -> generator.generate("out"));

            writer.verify(() ->
                    FileWriterUtils.writeToFile(anyString(), eq(GeneratorConstants.DefaultPackageLayout.SWAGGER), anyString(), anyString()), never()
            );
        }
    }
}
