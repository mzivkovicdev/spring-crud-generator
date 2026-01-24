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
import dev.markozivkovic.codegen.utils.AuditUtils;
import dev.markozivkovic.codegen.utils.FieldUtils;
import dev.markozivkovic.codegen.utils.ModelNameUtils;
import dev.markozivkovic.codegen.utils.PackageUtils;
import dev.markozivkovic.codegen.utils.StringUtils;

public class BusinessServiceImports {

    private BusinessServiceImports() {}
    
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
        ImportCommon.importListAndSetForSimpleCollection(modelDefinition, imports);
        
        if (modelDefinition.getAudit() != null) {
            ImportCommon.addIf(
                Objects.nonNull(modelDefinition.getAudit()) && modelDefinition.getAudit().isEnabled(),
                imports,
                AuditUtils.resolveAuditingImport(modelDefinition.getAudit().getType())
            );
        }
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
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

    /**
     * Computes the necessary imports for the given model definition for the test base, including imports for the types of its fields,
     * as well as imports for the types of its relations, if any.
     *
     * @param modelDefinition The model definition containing field information used to determine necessary imports.
     * @return A string containing the necessary import statements for the model.
     */
    public static String getTestBaseImport(final ModelDefinition modelDefinition) {

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

        ImportCommon.addIf(hasLists, imports, ImportConstants.Java.LIST);

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
     * Computes the necessary imports for the given model definition, including the enums if any exist, the model itself,
     * the related service, and any related models.
     *
     * @param modelDefinition      the model definition containing the class name, table name, and field definitions
     * @param outputDir            the directory where the generated code will be written
     * @param importScope          the import scope
     * @param packageConfiguration the package configuration
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeModelsEnumsAndServiceImports(final ModelDefinition modelDefinition, final String outputDir,
            final BusinessServiceImportScope importScope, final PackageConfiguration packageConfiguration) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final List<FieldDefinition> relationModels = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final String enumsImport = ModelImports.computeEnumsAndHelperEntitiesImport(modelDefinition, outputDir, packageConfiguration);

        imports.add(enumsImport);
        
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeServicePackage(packagePath, packageConfiguration), String.format("%sService", modelWithoutSuffix))));

        relationModels.forEach(relation -> {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), relation.getType())));
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeServicePackage(packagePath, packageConfiguration), ModelNameUtils.stripSuffix(relation.getType()) + "Service")));
        });

        if (GeneratorContext.isGenerated(GeneratorContextKeys.RETRYABLE_ANNOTATION) && BusinessServiceImportScope.BUSINESS_SERVICE.equals(importScope)) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeAnnotationPackage(packagePath, packageConfiguration), GeneratorConstants.Transaction.OPTIMISTIC_LOCKING_RETRY)));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary import statements for the generated test business service.
     *
     * @param isInstancioEnabled whether Instancio is enabled
     * @return A string containing the necessary import statements for the generated test business service.
     */
    public static String computeTestBusinessServiceImports(final boolean isInstancioEnabled) {

        final Set<String> imports = new LinkedHashSet<>();

        if (isInstancioEnabled) {
            imports.add(String.format(IMPORT, ImportConstants.INSTANCIO.INSTANCIO));
        }
        imports.add(String.format(IMPORT, ImportConstants.JUnit.AFTER_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.BEFORE_EACH));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.TEST));
        imports.add(String.format(IMPORT, ImportConstants.JUnit.EXTEND_WITH));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.MOCKITO_BEAN));
        imports.add(String.format(IMPORT, ImportConstants.SpringTest.SPRING_EXTENSION));

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    public enum BusinessServiceImportScope {
        BUSINESS_SERVICE,
        BUSINESS_SERVICE_TEST
    }
}
