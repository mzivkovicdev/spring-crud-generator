package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;
import static com.markozivkovic.codegen.constants.ImportConstants.PACKAGE;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.AuditUtils;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.FreeMarkerTemplateProcessorUtils;
import com.markozivkovic.codegen.utils.ImportUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;
import com.markozivkovic.codegen.utils.TemplateContextUtils;

public class TransferObjectGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TransferObjectGenerator.class);

    private static boolean PAGE_TO_GENERATED = false;

    private final CrudConfiguration configuration;
    private final List<ModelDefinition> entities;

    public TransferObjectGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entities) {
        this.entities = entities;
        this.configuration = configuration;
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

                    final String jsonFieldName = FieldUtils.extractJsonFieldName(field);
                    final ModelDefinition jsonModel = this.entities.stream()
                            .filter(model -> model.getName().equals(jsonFieldName))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                String.format(
                                    "JSON model not found: %s", jsonFieldName
                                )
                            ));
                    final String restHelperPackagePath = PackageUtils.join(
                            packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.REST, GeneratorConstants.DefaultPackageLayout.HELPERS
                    );
                    final String fileHelperPath = FileUtils.join(
                        GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.REST, GeneratorConstants.DefaultPackageLayout.HELPERS
                    );
                    this.generateHelperTO(jsonModel, outputDir, restHelperPackagePath, fileHelperPath);

                    if (configuration != null && configuration.getGraphQl() != null && configuration.getGraphQl()) {
                        final String graphQlHelperPackagePath = PackageUtils.join(
                                packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.GRAPHQL, GeneratorConstants.DefaultPackageLayout.HELPERS
                        );
                        final String fileGraphqlPath = FileUtils.join(
                                GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.GRAPHQL, GeneratorConstants.DefaultPackageLayout.HELPERS
                        );
                        this.generateHelperTO(jsonModel, outputDir, graphQlHelperPackagePath, fileGraphqlPath);
                    }
                });

        final String packagePathRest = PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.REST);
        final String filePathRest = FileUtils.join(GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.REST);
        this.generateTO(modelDefinition, outputDir, packagePathRest, filePathRest, false);
        
        if (configuration != null && configuration.getGraphQl() != null && configuration.getGraphQl()) {
            final String packagePathGraphql = PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.GRAPHQL);
            final String filePathGraphql = FileUtils.join(GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.GRAPHQL);
            this.generateTO(modelDefinition, outputDir, packagePathGraphql, filePathGraphql, true);
            this.generateCreateTO(modelDefinition, outputDir, packagePathGraphql, filePathGraphql);
            this.generateUpdateTO(modelDefinition, outputDir, packagePathGraphql, filePathGraphql);
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
                final String subDir, final boolean graphqlTOs) {

        generateTO(
            modelDefinition, outputDir, packagePath, subDir, String.format("%sTO", ModelNameUtils.stripSuffix(modelDefinition.getName())),
            TemplateContextUtils.computeTransferObjectContext(modelDefinition), false, !graphqlTOs, graphqlTOs
        );
    }

    /**
     * Generates a create transfer object for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @param outputDir       the directory where the generated class will be written
     * @param packagePath     the package path of the directory where the generated class will be written
     * @param subDir          the sub directory where the generated class will be written
     */
    private void generateCreateTO(final ModelDefinition modelDefinition, final String outputDir, final String packagePath, final String subDir) {

        generateTO(
            modelDefinition, outputDir, packagePath, subDir, String.format("%sCreateTO", ModelNameUtils.stripSuffix(modelDefinition.getName())),
            TemplateContextUtils.computeCreateTransferObjectContext(modelDefinition, this.entities), true, false, true
        );
    }

    /**
     * Generates an update transfer object for the given model definition.
     * 
     * @param modelDefinition the model definition containing the class and field details
     * @param outputDir       the directory where the generated class will be written
     * @param packagePath     the package path of the directory where the generated class will be written
     * @param subDir          the sub directory where the generated class will be written
     */
    private void generateUpdateTO(final ModelDefinition modelDefinition, final String outputDir, final String packagePath, final String subDir) {

        generateTO(
            modelDefinition, outputDir, packagePath, subDir, String.format("%sUpdateTO", ModelNameUtils.stripSuffix(modelDefinition.getName())),
            TemplateContextUtils.computeUpdateTransferObjectContext(modelDefinition), false, false, true
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
     */
    private void generateTO(final ModelDefinition modelDefinition, final String outputDir, final String packagePath, final String subDir,
            final String transferObjName, final Map<String, Object> toContext, final Boolean relationIdsImport, final boolean restTOs,
            final boolean graphql) {

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, packagePath));

        final String imports = ImportUtils.getBaseImport(modelDefinition, entities, relationIdsImport);
        if (Objects.nonNull(modelDefinition.getAudit()) && modelDefinition.getAudit().isEnabled()) {
            sb.append(String.format(IMPORT, AuditUtils.resolveAuditingImport(modelDefinition.getAudit().getType())));
        }
        sb.append(imports);

        final String enumAndHelperEntityImports = ImportUtils.computeEnumsAndHelperEntitiesImport(
                modelDefinition, outputDir, true, restTOs, graphql
        );
        
        if (StringUtils.isNotBlank(enumAndHelperEntityImports)) {
            sb.append(enumAndHelperEntityImports)
                .append("\n");
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

        final String imports = ImportUtils.getBaseImport(modelDefinition, false, false);
        sb.append(imports);

        final String enumAndHelperEntityImports = ImportUtils.computeEnumsAndHelperEntitiesImport(
                modelDefinition, outputDir, false, false, false
        );
        
        if (StringUtils.isNotBlank(enumAndHelperEntityImports)) {
            sb.append(enumAndHelperEntityImports)
                .append("\n");
        }

        final Map<String, Object> toContext = TemplateContextUtils.computeTransferObjectContext(modelDefinition);
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
            final String packagePathRest = PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.REST);
            sb.append(String.format(PACKAGE, packagePathRest));
    
            final String transferObjName = String.format("%sInputTO", ModelNameUtils.stripSuffix(relationModelDefinition.getName()));
            final FieldDefinition idField = FieldUtils.extractIdField(relationModelDefinition.getFields());
    
            if (FieldUtils.isIdFieldUUID(idField)) {
                sb.append(String.format(IMPORT, ImportConstants.Java.UUID))
                        .append("\n");
            }
    
            final Map<String, Object> toContext = TemplateContextUtils.computeInputTransferObjectContext(relationModelDefinition);
            final String transferObjectTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                    "transferobject/transfer-object-input-template.ftl", toContext
            );
    
            sb.append(transferObjectTemplate);
            final String filePath = FileUtils.join(
                    GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, GeneratorConstants.DefaultPackageLayout.REST
            );
    
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
    private static void generatePageTO(final String packagePath, final String outputDir) {

        if (!PAGE_TO_GENERATED) {

            final StringBuilder pageSb = new StringBuilder();
            pageSb.append(String.format(PACKAGE, PackageUtils.join(packagePath, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS)));

            final String pageTOObjectTemplate = FreeMarkerTemplateProcessorUtils.processTemplate(
                    "transferobject/page-transfer-object-template.ftl",
                    Map.of()
            );

            pageSb.append(pageTOObjectTemplate);

            FileWriterUtils.writeToFile(outputDir, GeneratorConstants.DefaultPackageLayout.TRANSFEROBJECTS, "PageTO", pageSb.toString());
            PAGE_TO_GENERATED = true;
        }
    }

}
