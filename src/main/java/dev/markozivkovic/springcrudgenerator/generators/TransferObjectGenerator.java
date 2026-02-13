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

package dev.markozivkovic.springcrudgenerator.generators;

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.IMPORT;
import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.imports.TransferObjectImports;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.templates.TransferObjectTemplateContext;
import dev.markozivkovic.springcrudgenerator.utils.AuditUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;

public class TransferObjectGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TransferObjectGenerator.class);

    private static boolean PAGE_TO_GENERATED = false;

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;
    private final PackageConfiguration packageConfiguration;

    public TransferObjectGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities,
                final PackageConfiguration packageConfiguration) {
        this.entities = entities;
        this.configuration = configuration;
        this.packageConfiguration = packageConfiguration;
    }


    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {

        if (FieldUtils.isModelUsedAsJsonField(modelDefinition, this.entities)) {
            return;
        }
        
        LOGGER.info("Generator transfer object for model: {}", modelDefinition.getName());

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        modelDefinition.getFields().stream()
                .filter(FieldUtils::isJsonField)
                .forEach(field -> {

                    final String jsonInnerElementType = FieldUtils.extractJsonInnerElementType(field);
                    final ModelDefinition jsonModel = this.entities.stream()
                            .filter(model -> model.getName().equals(jsonInnerElementType))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                String.format(
                                    "JSON model not found: %s", jsonInnerElementType
                                )
                            ));
                    final String restHelperPackagePath = PackageUtils.computeHelperRestTransferObjectPackage(packagePath, packageConfiguration);
                    final String fileHelperPath = PackageUtils.computeHelperRestTransferObjectSubPackage(packageConfiguration);
                    this.generateHelperTO(jsonModel, outputDir, restHelperPackagePath, fileHelperPath);

                    if (configuration != null && configuration.getGraphql() != null && Boolean.TRUE.equals(this.configuration.getGraphql().getEnabled())) {
                        final String graphQlHelperPackagePath = PackageUtils.computeHelperGraphqlTransferObjectPackage(packagePath, packageConfiguration);
                        final String fileGraphqlPath = PackageUtils.computeHelperGraphqlTransferObjectSubPackage(packageConfiguration);
                        this.generateHelperTO(jsonModel, outputDir, graphQlHelperPackagePath, fileGraphqlPath);
                    }
                });

        final String packagePathRest = PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration);
        final String filePathRest = PackageUtils.computeRestTransferObjectSubPackage(packageConfiguration);
        this.generateTO(modelDefinition, outputDir, packagePathRest, filePathRest, TransferObjectTarget.REST);
        if (Objects.isNull(this.configuration.getOpenApi()) || !Boolean.TRUE.equals(this.configuration.getOpenApi().getGenerateResources())) {
            this.generateCreateTO(modelDefinition, outputDir, packagePathRest, filePathRest, TransferObjectTarget.REST);
            this.generateUpdateTO(modelDefinition, outputDir, packagePathRest, filePathRest, TransferObjectTarget.REST);
        }
        
        if (configuration != null && configuration.getGraphql() != null && Boolean.TRUE.equals(this.configuration.getGraphql().getEnabled())) {
            final String packagePathGraphql = PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration);
            final String filePathGraphql = PackageUtils.computeGraphqlTransferObjectSubPackage(packageConfiguration);
            this.generateTO(modelDefinition, outputDir, packagePathGraphql, filePathGraphql, TransferObjectTarget.GRAPHQL);
            this.generateCreateTO(modelDefinition, outputDir, packagePathGraphql, filePathGraphql, TransferObjectTarget.GRAPHQL);
            this.generateUpdateTO(modelDefinition, outputDir, packagePathGraphql, filePathGraphql, TransferObjectTarget.GRAPHQL);
        }

        generatePageTO(packagePath, outputDir);
        generateInputTO(modelDefinition, outputDir, packagePath);
        
        LOGGER.info("Generator transfer object for model: {}", modelDefinition.getName());
    }

    /**
     * Generates a transfer object for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @param outputDir       the directory where the generated class will be written
     * @param packagePath     the package path of the directory where the generated class will be written
     * @param subDir          the sub directory where the generated class will be written
     * @param graphqlTOs      whether to generate GraphQL transfer objects
     */
    private void generateTO(final ModelDefinition modelDefinition, final String outputDir, final String packagePath,
                final String subDir, final TransferObjectTarget target) {

        generateTO(
            modelDefinition, outputDir, packagePath, subDir, String.format("%sTO", ModelNameUtils.stripSuffix(modelDefinition.getName())),
            TransferObjectTemplateContext.computeTransferObjectContext(modelDefinition), TransferObjectType.BASE, target
        );
    }

    /**
     * Generates a create transfer object for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @param outputDir       the directory where the generated class will be written
     * @param packagePath     the package path of the directory where the generated class will be written
     * @param subDir          the sub directory where the generated class will be written
     * @param target          the target of the transfer object
     */
    private void generateCreateTO(final ModelDefinition modelDefinition, final String outputDir, final String packagePath,
                final String subDir, final TransferObjectTarget target) {

        if (!FieldUtils.isAnyFieldId(modelDefinition.getFields())) {
            return;
        }

        switch (target) {
            case GRAPHQL -> this.generateTO(
                modelDefinition, outputDir, packagePath, subDir, String.format("%sCreateTO", ModelNameUtils.stripSuffix(modelDefinition.getName())),
                TransferObjectTemplateContext.computeCreateTransferObjectContext(modelDefinition, this.entities), TransferObjectType.CREATE, target
            );
            case REST -> this.generateTO(
                modelDefinition, outputDir, packagePath, subDir, String.format("%sCreateTO", ModelNameUtils.stripSuffix(modelDefinition.getName())),
                TransferObjectTemplateContext.computeCreateTransferObjectContext(modelDefinition), TransferObjectType.CREATE, target
            );
        }
    }

    /**
     * Generates an update transfer object for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @param outputDir       the directory where the generated class will be written
     * @param packagePath     the package path of the directory where the generated class will be written
     * @param subDir          the sub directory where the generated class will be written
     * @param target          the target of the transfer object
     */
    private void generateUpdateTO(final ModelDefinition modelDefinition, final String outputDir, final String packagePath, final String subDir,
                final TransferObjectTarget target) {

        if (!FieldUtils.isAnyFieldId(modelDefinition.getFields())) {
            return;
        }

        final String transferObjName = String.format("%sUpdateTO", ModelNameUtils.stripSuffix(modelDefinition.getName()));
        final Map<String, Object> toContext = TransferObjectTemplateContext.computeUpdateTransferObjectContext(modelDefinition);

        generateTO(
            modelDefinition, outputDir, packagePath, subDir, transferObjName, toContext, TransferObjectType.UPDATE, target
        );
    }

    /**
     * Generates a transfer object for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @param outputDir       the directory where the generated class will be written
     * @param packagePath     the package path of the directory where the generated class will be written
     * @param subDir          the sub directory where the generated class will be written
     * @param transferObjName the name of the transfer object
     * @param toContext       the map of context for the transfer object template
     * @param type            the type of the transfer object
     * @param target          the target of the transfer object
     */
    private void generateTO(final ModelDefinition modelDefinition, final String outputDir, final String packagePath, final String subDir,
            final String transferObjName, final Map<String, Object> toContext, final TransferObjectType type, final TransferObjectTarget target) {

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, packagePath));

        final String imports = TransferObjectImports.getBaseImport(modelDefinition, entities, type);
        if (Objects.nonNull(modelDefinition.getAudit()) && modelDefinition.getAudit().isEnabled()
                    && (!TransferObjectType.CREATE.equals(type) && !TransferObjectType.UPDATE.equals(type))) {
            sb.append(String.format(IMPORT, AuditUtils.resolveAuditingImport(modelDefinition.getAudit().getType())));
        }
        sb.append(imports);

        if (FieldUtils.hasAnyColumnValidation(modelDefinition.getFields()) || FieldUtils.hasAnyFieldValidation(modelDefinition.getFields())) {
            sb.append(TransferObjectImports.computeValidationImport(modelDefinition));
        }

        final String enumAndHelperEntityImports = TransferObjectImports.computeEnumsAndHelperEntitiesImport(
                modelDefinition, outputDir, true, target, packageConfiguration
        );
        
        if (StringUtils.isNotBlank(enumAndHelperEntityImports)) {
            sb.append(enumAndHelperEntityImports)
                .append(System.lineSeparator());
        }

        final String transferObjectTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("transferobject/transfer-object-template.ftl", toContext);
        
        sb.append(transferObjectTemplate);

        FileWriterUtils.writeToFile(outputDir, subDir, transferObjName, sb.toString());
    }

    /**
     * Generates a helper transfer object for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @param outputDir the directory where the generated class will be written
     * @param packagePath the package path of the directory where the generated class will be written
     */
    private void generateHelperTO(final ModelDefinition modelDefinition, final String outputDir, final String packagePath, final String subDir) {
        
        final String transferObjName = String.format("%sTO", ModelNameUtils.stripSuffix(modelDefinition.getName()));

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, packagePath));

        final String imports = TransferObjectImports.getBaseImport(modelDefinition);
        sb.append(imports);

        if (FieldUtils.hasAnyColumnValidation(modelDefinition.getFields()) || FieldUtils.hasAnyFieldValidation(modelDefinition.getFields())) {
            sb.append(TransferObjectImports.computeValidationImport(modelDefinition));
        }

        final String enumAndHelperEntityImports = TransferObjectImports.computeEnumsAndHelperEntitiesImport(
                modelDefinition, outputDir, false, null, packageConfiguration
        );
        
        if (StringUtils.isNotBlank(enumAndHelperEntityImports)) {
            sb.append(enumAndHelperEntityImports)
                .append("%n");
        }

        final Map<String, Object> toContext = TransferObjectTemplateContext.computeTransferObjectContext(modelDefinition);
        final String transferObjectTemplate = FreeMarkerTemplateProcessorUtils.processTemplate("transferobject/transfer-object-template.ftl", toContext);
        
        sb.append(transferObjectTemplate);

        FileWriterUtils.writeToFile(outputDir, subDir, transferObjName, sb.toString());
    }

    /**
     * Generates input transfer objects for each relation of the given model.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @param outputDir the directory where the generated class will be written
     * @param packagePath the package path to use as the prefix for the generated class
     */
    private void generateInputTO(final ModelDefinition modelDefinition, final String outputDir, final String packagePath) {
        
        if (FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            return;
        }

        final List<FieldDefinition> relations = FieldUtils.extractRelationFields(modelDefinition.getFields());

        relations.forEach(relation -> {

            final ModelDefinition relationModelDefinition = entities.stream()
                    .filter(model -> model.getName().equals(relation.getType()))
                    .findFirst()
                    .orElseThrow();

            final StringBuilder sb = new StringBuilder();
            final String packagePathRest = PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration);
            sb.append(String.format(PACKAGE, packagePathRest));
    
            final String transferObjName = String.format("%sInputTO", ModelNameUtils.stripSuffix(relationModelDefinition.getName()));
            final FieldDefinition idField = FieldUtils.extractIdField(relationModelDefinition.getFields());
    
            if (FieldUtils.isIdFieldUUID(idField)) {
                sb.append(String.format(IMPORT, ImportConstants.Java.UUID))
                        .append("%n");
            }
    
            final Map<String, Object> toContext = TransferObjectTemplateContext.computeInputTransferObjectContext(relationModelDefinition);
            final String transferObjectTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                    "transferobject/transfer-object-input-template.ftl", toContext
            );
    
            sb.append(transferObjectTemplate);
            final String filePath = PackageUtils.computeRestTransferObjectSubPackage(packageConfiguration);
    
            FileWriterUtils.writeToFile(outputDir, filePath, transferObjName, sb.toString());
        });
    }

    /**
     * Generates the PageTO record. This record is used as a transfer object for paged resources.
     * It contains the total number of pages, the total number of elements, the page size, the page number,
     * and the content of the page.
     * 
     * @param packagePath the package path to use as the prefix for the generated class
     * @param outputDir the directory where the generated class will be written
     */
    private void generatePageTO(final String packagePath, final String outputDir) {

        if (!PAGE_TO_GENERATED) {

            final StringBuilder pageSb = new StringBuilder();
            pageSb.append(String.format(PACKAGE, PackageUtils.computeTransferObjectPackage(packagePath, packageConfiguration)));

            final String pageTOObjectTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                    "transferobject/page-transfer-object-template.ftl",
                    Map.of()
            );

            pageSb.append(pageTOObjectTemplate);

            FileWriterUtils.writeToFile(outputDir, PackageUtils.computeTransferObjectSubPackage(packageConfiguration), "PageTO", pageSb.toString());
            PAGE_TO_GENERATED = true;
        }
    }

    public enum TransferObjectTarget {
        REST, GRAPHQL
    }

    public enum TransferObjectType {
        INPUT, CREATE, UPDATE, BASE
    }

}
