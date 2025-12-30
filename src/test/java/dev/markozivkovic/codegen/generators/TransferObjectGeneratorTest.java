package dev.markozivkovic.codegen.generators;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import dev.markozivkovic.codegen.generators.TransferObjectGenerator.TransferObjectTarget;
import dev.markozivkovic.codegen.generators.TransferObjectGenerator.TransferObjectType;
import dev.markozivkovic.codegen.imports.TransferObjectImports;
import dev.markozivkovic.codegen.models.AuditDefinition;
import dev.markozivkovic.codegen.models.AuditDefinition.AuditTypeEnum;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.CrudConfiguration.GraphQLDefinition;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.templates.TransferObjectTemplateContext;
import dev.markozivkovic.codegen.utils.AuditUtils;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.FileWriterUtils;
import dev.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;

class TransferObjectGeneratorTest {

    private ModelDefinition newModel(final String name, final List<FieldDefinition> fields) {
        final ModelDefinition m = mock(ModelDefinition.class);
        when(m.getName()).thenReturn(name);
        when(m.getFields()).thenReturn(fields);
        return m;
    }

    @BeforeEach
    void resetPageToGenerated() throws Exception {
        final Field f = TransferObjectGenerator.class.getDeclaredField("PAGE_TO_GENERATED");
        f.setAccessible(true);
        f.set(null, false);
    }

    @Test
    void generate_shouldSkipWhenModelIsUsedAsJsonField() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final ModelDefinition model = newModel("UserEntity", List.of());
        final List<ModelDefinition> entities = List.of(model);

        final TransferObjectGenerator generator =
                new TransferObjectGenerator(cfg, entities, pkgCfg);

        final String outputDir = "out";

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<TransferObjectImports> toImports = mockStatic(TransferObjectImports.class);
             final MockedStatic<TransferObjectTemplateContext> toCtx = mockStatic(TransferObjectTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(model, entities))
                      .thenReturn(true);

            generator.generate(model, outputDir);

            fieldUtils.verify(() -> FieldUtils.isModelUsedAsJsonField(model, entities));

            pkg.verifyNoInteractions();
            nameUtils.verifyNoInteractions();
            toImports.verifyNoInteractions();
            toCtx.verifyNoInteractions();
            tpl.verifyNoInteractions();
            writer.verifyNoInteractions();
            auditUtils.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldGenerateHelperRestAndGraphQlAndBaseCreateUpdateAndPageTO() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        
        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(cfg.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(true);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition mainJsonField = mock(FieldDefinition.class);

        final ModelDefinition mainModel = newModel("UserEntity", List.of(mainJsonField));
        final ModelDefinition jsonModel = newModel("AddressEntity", List.of());

        final List<ModelDefinition> entities = List.of(mainModel, jsonModel);

        final TransferObjectGenerator generator =
                new TransferObjectGenerator(cfg, entities, pkgCfg);

        final String outputDir = "out";

        final List<String> writtenClassNames = new ArrayList<>();
        final List<String> writtenSubDirs = new ArrayList<>();

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<TransferObjectImports> toImports = mockStatic(TransferObjectImports.class);
             final MockedStatic<TransferObjectTemplateContext> toCtx = mockStatic(TransferObjectTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(mainModel, entities))
                      .thenReturn(false);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
               .thenReturn("com.example.app");

            fieldUtils.when(() -> FieldUtils.isJsonField(mainJsonField))
                      .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(mainJsonField))
                      .thenReturn("AddressEntity");

            pkg.when(() -> PackageUtils.computeHelperRestTransferObjectPackage("com.example.app", pkgCfg))
               .thenReturn("com.example.app.to.helper.rest");
            pkg.when(() -> PackageUtils.computeHelperRestTransferObjectSubPackage(pkgCfg))
               .thenReturn("to/helper/rest");

            pkg.when(() -> PackageUtils.computeHelperGraphqlTransferObjectPackage("com.example.app", pkgCfg))
               .thenReturn("com.example.app.to.helper.graphql");
            pkg.when(() -> PackageUtils.computeHelperGraphqlTransferObjectSubPackage(pkgCfg))
               .thenReturn("to/helper/graphql");

            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.example.app", pkgCfg))
               .thenReturn("com.example.app.to.rest");
            pkg.when(() -> PackageUtils.computeRestTransferObjectSubPackage(pkgCfg))
               .thenReturn("to/rest");

            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectPackage("com.example.app", pkgCfg))
               .thenReturn("com.example.app.to.graphql");
            pkg.when(() -> PackageUtils.computeGraphqlTransferObjectSubPackage(pkgCfg))
               .thenReturn("to/graphql");

            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.example.app", pkgCfg))
               .thenReturn("com.example.app.to");
            pkg.when(() -> PackageUtils.computeTransferObjectSubPackage(pkgCfg))
               .thenReturn("to");

            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity"))
                     .thenReturn("User");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("AddressEntity"))
                     .thenReturn("Address");

            fieldUtils.when(() -> FieldUtils.hasAnyColumnValidation(mainModel.getFields()))
                      .thenReturn(false);
            fieldUtils.when(() -> FieldUtils.hasAnyColumnValidation(jsonModel.getFields()))
                      .thenReturn(false);

            toImports.when(() -> TransferObjectImports.getBaseImport(mainModel, entities, TransferObjectType.BASE))
                     .thenReturn("BASE_IMPORT_MAIN;");
            toImports.when(() -> TransferObjectImports.getBaseImport(mainModel, entities, TransferObjectType.CREATE))
                     .thenReturn("BASE_IMPORT_CREATE;");
            toImports.when(() -> TransferObjectImports.getBaseImport(mainModel, entities, TransferObjectType.UPDATE))
                     .thenReturn("BASE_IMPORT_UPDATE;");
            toImports.when(() -> TransferObjectImports.getBaseImport(jsonModel))
                     .thenReturn("BASE_IMPORT_HELPER;");

            toImports.when(() -> TransferObjectImports.computeEnumsAndHelperEntitiesImport(
                    mainModel, outputDir, true, TransferObjectTarget.REST, pkgCfg))
                     .thenReturn("");
            toImports.when(() -> TransferObjectImports.computeEnumsAndHelperEntitiesImport(
                    mainModel, outputDir, true, TransferObjectTarget.GRAPHQL, pkgCfg))
                     .thenReturn("");
            toImports.when(() -> TransferObjectImports.computeEnumsAndHelperEntitiesImport(
                    jsonModel, outputDir, false, null, pkgCfg))
                     .thenReturn("");

            toImports.when(() -> TransferObjectImports.computeValidationImport(mainModel))
                     .thenReturn("");
            toImports.when(() -> TransferObjectImports.computeValidationImport(jsonModel))
                     .thenReturn("");

            when(mainModel.getAudit()).thenReturn(null);
            when(jsonModel.getAudit()).thenReturn(null);

            fieldUtils.when(() -> FieldUtils.isAnyFieldId(mainModel.getFields()))
                    .thenReturn(true);

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(mainModel.getFields()))
                    .thenReturn(Collections.emptyList());

            toCtx.when(() -> TransferObjectTemplateContext.computeTransferObjectContext(mainModel))
                .thenReturn(new HashMap<>());
            toCtx.when(() -> TransferObjectTemplateContext.computeTransferObjectContext(jsonModel))
                .thenReturn(new HashMap<>());

            toCtx.when(() -> TransferObjectTemplateContext.computeCreateTransferObjectContext(mainModel, entities))
                .thenReturn(new HashMap<>());
            toCtx.when(() -> TransferObjectTemplateContext.computeUpdateTransferObjectContext(mainModel))
                .thenReturn(new HashMap<>());

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("transferobject/transfer-object-template.ftl"), anyMap()))
                .thenReturn("/* TO TEMPLATE */");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("transferobject/page-transfer-object-template.ftl"), anyMap()))
                .thenReturn("/* PAGE TO TEMPLATE */");

            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), anyString(), anyString(), anyString()))
                    .thenAnswer(inv -> {
                        final String subdir = inv.getArgument(1, String.class);
                        final String cls = inv.getArgument(2, String.class);
                        writtenSubDirs.add(subdir);
                        writtenClassNames.add(cls);
                        return null;
                    });

            generator.generate(mainModel, outputDir);
        }

        assertTrue(writtenClassNames.contains("AddressTO"), "Expected helper AddressTO to be generated (at least once)");
        assertTrue(writtenClassNames.contains("UserTO"), "Expected UserTO (REST/GraphQL) to be generated");
        assertTrue(writtenClassNames.contains("UserCreateTO"), "Expected UserCreateTO to be generated");
        assertTrue(writtenClassNames.contains("UserUpdateTO"), "Expected UserUpdateTO to be generated");
        assertTrue(writtenClassNames.contains("PageTO"), "Expected PageTO to be generated");
        assertTrue(writtenSubDirs.contains("to/helper/rest"), "Expected helper REST subdir");
        assertTrue(writtenSubDirs.contains("to/helper/graphql"), "Expected helper GraphQL subdir");
        assertTrue(writtenSubDirs.contains("to/rest"), "Expected REST TO subdir");
        assertTrue(writtenSubDirs.contains("to/graphql"), "Expected GraphQL TO subdir");
        assertTrue(writtenSubDirs.contains("to"), "Expected general TO subdir for PageTO");
    }

    @Test
    void generate_shouldThrowWhenJsonModelNotFound() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        
        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(cfg.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(false);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);

        final FieldDefinition jsonField = mock(FieldDefinition.class);
        final ModelDefinition mainModel = newModel("UserEntity", List.of(jsonField));
        final List<ModelDefinition> entities = List.of(mainModel);

        final TransferObjectGenerator generator = new TransferObjectGenerator(cfg, entities, pkgCfg);

        final String outputDir = "out";

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
             final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
             final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
             final MockedStatic<TransferObjectImports> toImports = mockStatic(TransferObjectImports.class);
             final MockedStatic<TransferObjectTemplateContext> toCtx = mockStatic(TransferObjectTemplateContext.class);
             final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
             final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
             final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(mainModel, entities))
                    .thenReturn(false);

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir))
                   .thenReturn("com.example.app");

            fieldUtils.when(() -> FieldUtils.isJsonField(jsonField))
                    .thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractJsonFieldName(jsonField))
                    .thenReturn("AddressEntity");

            fieldUtils.when(() -> FieldUtils.extractRelationTypes(mainModel.getFields()))
                    .thenReturn(Collections.emptyList());
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(mainModel.getFields()))
                    .thenReturn(false);

            assertThrows(IllegalArgumentException.class, () -> generator.generate(mainModel, outputDir));

            writer.verifyNoInteractions();
        }
    }

    @Test
    void generate_shouldAddAuditingImport_forBaseTransferObject_whenAuditEnabled_andNotCreateOrUpdate() {

        final CrudConfiguration cfg = mock(CrudConfiguration.class);
        
        final GraphQLDefinition graphQlDef = mock(GraphQLDefinition.class);
        when(cfg.getGraphql()).thenReturn(graphQlDef);
        when(graphQlDef.getEnabled()).thenReturn(false);

        final PackageConfiguration pkgCfg = mock(PackageConfiguration.class);
        final ModelDefinition mainModel = newModel("UserEntity", List.of());
        final List<ModelDefinition> entities = List.of(mainModel);

        final AuditDefinition audit = mock(AuditDefinition.class);
        when(audit.isEnabled()).thenReturn(true);
        when(audit.getType()).thenReturn(AuditTypeEnum.LOCALDATETIME);
        when(mainModel.getAudit()).thenReturn(audit);

        final TransferObjectGenerator generator = new TransferObjectGenerator(cfg, entities, pkgCfg);
        final String outputDir = "out";

        try (final MockedStatic<FieldUtils> fieldUtils = mockStatic(FieldUtils.class);
            final MockedStatic<PackageUtils> pkg = mockStatic(PackageUtils.class);
            final MockedStatic<ModelNameUtils> nameUtils = mockStatic(ModelNameUtils.class);
            final MockedStatic<TransferObjectImports> toImports = mockStatic(TransferObjectImports.class);
            final MockedStatic<TransferObjectTemplateContext> toCtx = mockStatic(TransferObjectTemplateContext.class);
            final MockedStatic<FreeMarkerTemplateProcessorUtils> tpl = mockStatic(FreeMarkerTemplateProcessorUtils.class);
            final MockedStatic<FileWriterUtils> writer = mockStatic(FileWriterUtils.class);
            final MockedStatic<AuditUtils> auditUtils = mockStatic(AuditUtils.class)) {

            fieldUtils.when(() -> FieldUtils.isModelUsedAsJsonField(mainModel, entities)).thenReturn(false);
            fieldUtils.when(() -> FieldUtils.isAnyFieldId(mainModel.getFields())).thenReturn(true);
            fieldUtils.when(() -> FieldUtils.extractRelationTypes(mainModel.getFields())).thenReturn(Collections.emptyList());

            pkg.when(() -> PackageUtils.getPackagePathFromOutputDir(outputDir)).thenReturn("com.example.app");
            pkg.when(() -> PackageUtils.computeRestTransferObjectPackage("com.example.app", pkgCfg))
                    .thenReturn("com.example.app.to.rest");
            pkg.when(() -> PackageUtils.computeRestTransferObjectSubPackage(pkgCfg)).thenReturn("to/rest");
            pkg.when(() -> PackageUtils.computeTransferObjectPackage("com.example.app", pkgCfg)).thenReturn("com.example.app.to");
            pkg.when(() -> PackageUtils.computeTransferObjectSubPackage(pkgCfg)).thenReturn("to");
            nameUtils.when(() -> ModelNameUtils.stripSuffix("UserEntity")).thenReturn("User");

            fieldUtils.when(() -> FieldUtils.hasAnyColumnValidation(mainModel.getFields()))
                    .thenReturn(false);

            toImports.when(() -> TransferObjectImports.getBaseImport(mainModel, entities, TransferObjectType.BASE)).thenReturn("BASE_IMPORTS\n");
            toImports.when(() -> TransferObjectImports.computeEnumsAndHelperEntitiesImport(eq(mainModel), eq(outputDir), anyBoolean(), any(), eq(pkgCfg)))
                    .thenReturn("");
            toImports.when(() -> TransferObjectImports.computeValidationImport(mainModel)).thenReturn("");

            toCtx.when(() -> TransferObjectTemplateContext.computeTransferObjectContext(mainModel)).thenReturn(new HashMap<>());

            auditUtils.when(() -> AuditUtils.resolveAuditingImport(AuditTypeEnum.LOCALDATETIME))
                    .thenReturn("org.springframework.data.annotation.CreatedDate");

            tpl.when(() -> FreeMarkerTemplateProcessorUtils.processTemplate(eq("transferobject/transfer-object-template.ftl"), anyMap()))
                    .thenReturn("/* TO TEMPLATE */");

            final AtomicReference<String> written = new AtomicReference<>();
            writer.when(() -> FileWriterUtils.writeToFile(eq(outputDir), anyString(), anyString(), anyString()))
                    .thenAnswer(inv -> {
                        written.set(inv.getArgument(3, String.class));
                        return null;
                    });

            generator.generate(mainModel, outputDir);

            auditUtils.verify(() -> AuditUtils.resolveAuditingImport(AuditTypeEnum.LOCALDATETIME), times(1));

            assertNotNull(written.get(), "Expected something to be written");
        }
    }
}
