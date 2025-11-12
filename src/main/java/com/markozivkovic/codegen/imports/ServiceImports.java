package com.markozivkovic.codegen.imports;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.markozivkovic.codegen.constants.GeneratorConstants;
import com.markozivkovic.codegen.constants.GeneratorConstants.DefaultPackageLayout;
import com.markozivkovic.codegen.constants.GeneratorConstants.GeneratorContextKeys;
import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.context.GeneratorContext;
import com.markozivkovic.codegen.imports.common.ImportCommon;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;

public class ServiceImports {

    private static final String INVALID_RESOURCE_STATE_EXCEPTION = "InvalidResourceStateException";
    private static final String RESOURCE_NOT_FOUND_EXCEPTION = "ResourceNotFoundException";
    
    private ServiceImports() {}

    /**
     * Computes the necessary imports for the given model definition, including imports for the types of its fields,
     * as well as imports for the types of its relations, if any.
     *
     * @param modelDefinition the model definition containing field information used to determine necessary imports.
     * @param importList      whether to include the java.util.List import.
     * @return A string containing the necessary import statements for the model.
     */
    public static String getBaseImport(final ModelDefinition modelDefinition, final boolean importList) {
        
        final StringBuilder sb = new StringBuilder();

        final List<FieldDefinition> fields = modelDefinition.getFields();
        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(FieldUtils.isAnyFieldBigDecimal(fields), imports, ImportConstants.Java.BIG_DECIMAL);
        ImportCommon.addIf(FieldUtils.isAnyFieldBigInteger(fields), imports, ImportConstants.Java.BIG_INTEGER);
        ImportCommon.addIf(FieldUtils.isAnyFieldLocalDate(fields), imports, ImportConstants.Java.LOCAL_DATE);
        ImportCommon.addIf(FieldUtils.isAnyFieldLocalDateTime(fields), imports, ImportConstants.Java.LOCAL_DATE_TIME);
        ImportCommon.addIf(FieldUtils.isAnyFieldUUID(fields), imports, ImportConstants.Java.UUID);
        
        final boolean hasLists = FieldUtils.isAnyRelationOneToMany(fields) ||
                FieldUtils.isAnyRelationManyToMany(fields);

        ImportCommon.addIf(hasLists || importList, imports, ImportConstants.Java.LIST);

        final String sortedImports = imports.stream()
                .map(imp -> String.format(IMPORT, imp))
                .sorted()
                .collect(Collectors.joining());

        sb.append(sortedImports);

        if (StringUtils.isNotBlank(sb.toString())) {
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Computes the base import statements for a JPA service.
     *
     * @param cache Whether to include the Spring caching annotations.
     * @return A string containing the necessary import statements for the base JPA service.
     */
    public static String computeJpaServiceBaseImport(final boolean cache) {

        final Set<String> imports = new LinkedHashSet<>();

        imports.add(String.format(IMPORT, ImportConstants.Logger.LOGGER));
        imports.add(String.format(IMPORT, ImportConstants.Logger.LOGGER_FACTORY));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE_REQUEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringStereotype.SERVICE));
        if (!GeneratorContext.isGenerated(GeneratorContextKeys.RETRYABLE_ANNOTATION)) {
            imports.add(String.format(IMPORT, ImportConstants.SpringTransaction.TRANSACTIONAL));
        }

        if (cache) {
            imports.add(String.format(IMPORT, ImportConstants.SpringCache.CACHEABLE));
            imports.add(String.format(IMPORT, ImportConstants.SpringCache.CACHE_EVICT));
            imports.add(String.format(IMPORT, ImportConstants.SpringCache.CACHE_PUT));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for the given model definition, including the enums if any exist, the model itself, the repository, and any related models.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param outputDir       the directory where the generated code will be written
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeModelsEnumsAndRepositoryImports(final ModelDefinition modelDefinition, final String outputDir) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final List<String> relationModels = modelDefinition.getFields().stream()
                .filter(field -> Objects.nonNull(field.getRelation()))
                .map(field -> field.getType())
                .collect(Collectors.toList());

        final String enumsImport = ModelImports.computeEnumsAndHelperEntitiesImport(modelDefinition, outputDir);

        imports.add(enumsImport);
        
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MODELS, modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.REPOSITORIES, String.format("%sRepository", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.EXCEPTIONS, RESOURCE_NOT_FOUND_EXCEPTION)));

        if (GeneratorContext.isGenerated(GeneratorContextKeys.RETRYABLE_ANNOTATION)) {
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.ANNOTATIONS, GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY)));
        }

        if (!relationModels.isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.EXCEPTIONS, INVALID_RESOURCE_STATE_EXCEPTION)));
        }

        relationModels.forEach(relation -> imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MODELS, relation))));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary import statements for the generated test service.
     *
     * @param modelDefinition     the model definition containing the class name, table name, and field definitions
     * @param entities            the list of all model definitions
     * @param isInstancioEnabled  whether Instancio is enabled
     * @return A string containing the necessary import statements for the generated test service.
     */
    public static String computeTestServiceImports(final ModelDefinition modelDefinition, final List<ModelDefinition> entities, 
            final boolean isInstancioEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        final boolean isAnyFieldEnum = FieldUtils.isAnyFieldEnum(modelDefinition.getFields());
        final boolean hasCollectionRelation = FieldUtils.hasCollectionRelation(modelDefinition, entities);

        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.BEFORE_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.EXTEND_WITH));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE_IMPL));
        imports.add(String.format(IMPORT, ImportConstants.SpringData.PAGE_REQUEST));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.SPRING_EXTENSION));

        ImportCommon.addIf(isInstancioEnabled, imports, String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        ImportCommon.addIf(isAnyFieldEnum, imports, String.format(IMPORT, ImportConstants.JUnit.Params.PARAMETERIZED_TEST));
        ImportCommon.addIf(isAnyFieldEnum, imports, String.format(IMPORT, ImportConstants.JUnit.Params.ENUM_SOURCE));
        ImportCommon.addIf(hasCollectionRelation, imports, String.format(IMPORT, ImportConstants.Java.COLLECTORS));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary import statements for the generated test service.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @return A string containing the necessary import statements for the generated test service.
     */
    public static String getTestBaseImport(final ModelDefinition modelDefinition) {
        
        final StringBuilder sb = new StringBuilder();

        final List<FieldDefinition> fields = modelDefinition.getFields();
        final Set<String> imports = new LinkedHashSet<>();
        imports.add(ImportConstants.Java.OBJECTS);
        imports.add(ImportConstants.Java.LIST);

        ImportCommon.addIf(FieldUtils.isAnyFieldBigDecimal(fields), imports, ImportConstants.Java.BIG_DECIMAL);
        ImportCommon.addIf(FieldUtils.isAnyFieldBigInteger(fields), imports, ImportConstants.Java.BIG_INTEGER);
        ImportCommon.addIf(FieldUtils.isAnyFieldLocalDate(fields), imports, ImportConstants.Java.LOCAL_DATE);
        ImportCommon.addIf(FieldUtils.isAnyFieldLocalDateTime(fields), imports, ImportConstants.Java.LOCAL_DATE_TIME);
        ImportCommon.addIf(FieldUtils.isAnyFieldUUID(fields), imports, ImportConstants.Java.UUID);

        final String sortedImports = imports.stream()
                .map(imp -> String.format(IMPORT, imp))
                .sorted()
                .collect(Collectors.joining());

        sb.append(sortedImports);

        if (StringUtils.isNotBlank(sb.toString())) {
            sb.append("\n");
        }

        return sb.toString();
    }

}
