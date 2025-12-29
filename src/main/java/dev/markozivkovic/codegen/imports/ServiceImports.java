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

package dev.markozivkovic.codegen.imports;

import static dev.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import dev.markozivkovic.codegen.constants.GeneratorConstants;
import dev.markozivkovic.codegen.constants.ImportConstants;
import dev.markozivkovic.codegen.constants.GeneratorConstants.GeneratorContextKeys;
import dev.markozivkovic.codegen.context.GeneratorContext;
import dev.markozivkovic.codegen.imports.common.ImportCommon;
import dev.markozivkovic.codegen.models.FieldDefinition;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;
import dev.markozivkovic.codegen.utils.StringUtils;

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

        ImportCommon.importListAndSetForSimpleCollection(modelDefinition, imports);
        
        final boolean hasLists = FieldUtils.isAnyRelationOneToMany(fields) ||
                FieldUtils.isAnyRelationManyToMany(fields);

        ImportCommon.addIf(hasLists || importList, imports, ImportConstants.Java.LIST);

        final String sortedImports = imports.stream()
                .map(imp -> String.format(IMPORT, imp))
                .sorted()
                .collect(Collectors.joining());

        sb.append(sortedImports);

        if (StringUtils.isNotBlank(sb.toString())) {
            sb.append(System.lineSeparator());
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
     * @param modelDefinition       the model definition containing the class name, table name, and field definitions
     * @param outputDir             the directory where the generated code will be written
     * @param importScope           the import scope
     * @param packageConfiguration  the package configuration
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeModelsEnumsAndRepositoryImports(final ModelDefinition modelDefinition, final String outputDir,
                final ServiceImportScope importScope, final PackageConfiguration packageConfiguration) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());

        final List<String> relationModels = modelDefinition.getFields().stream()
                .filter(field -> Objects.nonNull(field.getRelation()))
                .map(field -> field.getType())
                .collect(Collectors.toList());

        if (ServiceImportScope.SERVICE.equals(importScope)) {
            final String enumsImport = ModelImports.computeEnumsAndHelperEntitiesImport(modelDefinition, outputDir, packageConfiguration);
            imports.add(enumsImport);
        }
        
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeRepositoryPackage(packagePath, packageConfiguration), String.format("%sRepository", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeExceptionPackage(packagePath, packageConfiguration), RESOURCE_NOT_FOUND_EXCEPTION)));

        if (GeneratorContext.isGenerated(GeneratorContextKeys.RETRYABLE_ANNOTATION) && ServiceImportScope.SERVICE.equals(importScope)) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeAnnotationPackage(packagePath, packageConfiguration), GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY)));
        }

        if (!relationModels.isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeExceptionPackage(packagePath, packageConfiguration), INVALID_RESOURCE_STATE_EXCEPTION)));
        }

        relationModels.forEach(relation -> imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), relation))));

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
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final Set<String> imports = new LinkedHashSet<>();
        imports.add(ImportConstants.Java.OPTIONAL);
        imports.add(ImportConstants.Java.LIST);
        ImportCommon.addIf(FieldUtils.isIdFieldUUID(idField), imports, ImportConstants.Java.UUID);
        
        ImportCommon.importListAndSetForSimpleCollection(modelDefinition, imports);

        final String sortedImports = imports.stream()
                .map(imp -> String.format(IMPORT, imp))
                .sorted()
                .collect(Collectors.joining());

        sb.append(sortedImports);

        if (StringUtils.isNotBlank(sb.toString())) {
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

    public enum ServiceImportScope {
        SERVICE,
        SERVICE_TEST
    }

}
