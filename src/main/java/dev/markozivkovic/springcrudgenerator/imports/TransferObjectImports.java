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

package dev.markozivkovic.springcrudgenerator.imports;

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.enums.BasicTypeEnum;
import dev.markozivkovic.springcrudgenerator.enums.SpecialTypeEnum;
import dev.markozivkovic.springcrudgenerator.generators.TransferObjectGenerator.TransferObjectTarget;
import dev.markozivkovic.springcrudgenerator.generators.TransferObjectGenerator.TransferObjectType;
import dev.markozivkovic.springcrudgenerator.imports.common.ImportCommon;
import dev.markozivkovic.springcrudgenerator.imports.common.ImportCommon.CollectionImplImportsMode;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;

public class TransferObjectImports {
    
    private TransferObjectImports() {}

    /**
     * Generates a string of import statements based on the fields present in the given model definition.
     *
     * @param modelDefinition The model definition containing field information used to determine necessary imports.
     * @return A string containing the necessary import statements for the model.
     */
    public static String getBaseImport(final ModelDefinition modelDefinition) {
        
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
     * Computes the necessary imports for the given model definition, including imports for the types of its fields,
     * as well as imports for the types of its relations, if any.
     *
     * @param modelDefinition the model definition containing field information used to determine necessary imports.
     * @param entities        the list of all model definitions, used to determine the necessary imports for relations.
     * @param type            the type of the generated transfer object
     * @return A string containing the necessary import statements for the model.
     */
    public static String getBaseImport(final ModelDefinition modelDefinition, final List<ModelDefinition> entities, 
            final TransferObjectType type) {

        final StringBuilder sb = new StringBuilder();

        final List<FieldDefinition> fields = modelDefinition.getFields();
        final Set<String> imports = new LinkedHashSet<>();

        ImportCommon.addIf(FieldUtils.isAnyFieldBigDecimal(fields), imports, ImportConstants.Java.BIG_DECIMAL);
        ImportCommon.addIf(FieldUtils.isAnyFieldBigInteger(fields), imports, ImportConstants.Java.BIG_INTEGER);
        ImportCommon.addIf(FieldUtils.isAnyFieldLocalDate(fields), imports, ImportConstants.Java.LOCAL_DATE);
        ImportCommon.addIf(FieldUtils.isAnyFieldLocalDateTime(fields), imports, ImportConstants.Java.LOCAL_DATE_TIME);
        ImportCommon.addIf(FieldUtils.isAnyFieldUUID(fields), imports, ImportConstants.Java.UUID);

        ImportCommon.importListAndSetForJsonFields(modelDefinition, imports, CollectionImplImportsMode.INTERFACES_ONLY);
        ImportCommon.importListAndSetForSimpleCollection(modelDefinition, imports);

        if (TransferObjectType.CREATE.equals(type) || TransferObjectType.INPUT.equals(type)) {
            modelDefinition.getFields().stream()
                .filter(field -> Objects.nonNull(field.getRelation()))
                .forEach(field -> {

                    final ModelDefinition relatedEntity = entities.stream()
                            .filter(entity -> entity.getName().equals(field.getType()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                String.format(
                                    "Related entity not found: %s", field.getType()
                                )
                            ));
                    final FieldDefinition idField = FieldUtils.extractIdField(relatedEntity.getFields());
                    ImportCommon.addIf(FieldUtils.isIdFieldUUID(idField), imports, ImportConstants.Java.UUID);
                });
        }
        
        if (TransferObjectType.BASE.equals(type) || TransferObjectType.CREATE.equals(type)) {
            final boolean hasLists = FieldUtils.isAnyRelationOneToMany(fields) ||
                    FieldUtils.isAnyRelationManyToMany(fields);
            ImportCommon.addIf(hasLists, imports, ImportConstants.Java.LIST);
        }

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
     * Computes the necessary imports for the given model definition, including imports for the validation constraints.
     *
     * @param modelDefinition the model definition containing field information used to determine necessary imports.
     * @return A string containing the necessary import statements for the model.
     */
    public static String computeValidationImport(final ModelDefinition modelDefinition) {

        final Set<String> imports = new LinkedHashSet<>();
        final List<FieldDefinition> fields = modelDefinition.getFields();
        final StringBuilder sb = new StringBuilder();

        ImportCommon.addIf(FieldUtils.isAnyFieldNonNullable(fields), imports, ImportConstants.Jakarta.NOT_NULL);
        ImportCommon.addIf(FieldUtils.hasAnyFieldLengthValidation(fields), imports, ImportConstants.Jakarta.SIZE);

        modelDefinition.getFields().forEach(field -> {
            
            if (Objects.isNull(field.getValidation())) return;

            final boolean basicType = BasicTypeEnum.isBasicType(field.getType());
            final boolean isCollection = SpecialTypeEnum.isCollectionType(field.getType());

            ImportCommon.addIf(Boolean.TRUE.equals(field.getValidation().isRequired()), imports, ImportConstants.Jakarta.NOT_NULL);
            
            if (basicType) computeBasicTypeValidationImport(field, imports);
            if (isCollection) computeCollectionValidationImport(field, imports);
        });

        final String sortedImports = imports.stream()
                .map(imp -> String.format(IMPORT, imp))
                .sorted()
                .collect(Collectors.joining());

        if (StringUtils.isNotBlank(sortedImports)) {
            sb.append(sortedImports);
            sb.append(System.lineSeparator());
        }
        
        return sb.toString();
    }

    /**
     * Computes the necessary imports for the given field definition, including imports for the size constraint.
     *
     * @param field the field definition containing validation information used to determine necessary imports.
     * @param imports the set of imports to add to.
     */
    private static void computeCollectionValidationImport(final FieldDefinition field, final Set<String> imports) {
        
        ImportCommon.addIf(Boolean.TRUE.equals(field.getValidation().getNotEmpty()), imports, ImportConstants.Jakarta.NOT_EMPTY);
        ImportCommon.addIf(Objects.nonNull(field.getValidation().getMinItems()), imports, ImportConstants.Jakarta.SIZE);
        ImportCommon.addIf(Objects.nonNull(field.getValidation().getMaxItems()), imports, ImportConstants.Jakarta.SIZE);
    }

    /**
     * Computes the necessary imports for the given field definition, including imports for the size constraint and
     * other basic type constraints.
     *
     * @param field the field definition containing validation information used to determine necessary imports.
     * @param imports the set of imports to add to.
     * @return the set of imports with the necessary imports added.
     */
    private static Set<String> computeBasicTypeValidationImport(final FieldDefinition field, final Set<String> imports) {

        final BasicTypeEnum basicFieldType = BasicTypeEnum.fromString(field.getType().trim());
        
        switch (basicFieldType) {
            case STRING:
                ImportCommon.addIf(Boolean.TRUE.equals(field.getValidation().isNotBlank()), imports, ImportConstants.Jakarta.NOT_BLANK);
                ImportCommon.addIf(Boolean.TRUE.equals(field.getValidation().isNotEmpty()), imports, ImportConstants.Jakarta.NOT_EMPTY);
                ImportCommon.addIf(Boolean.TRUE.equals(field.getValidation().isEmail()), imports, ImportConstants.Jakarta.EMAIL);
                ImportCommon.addIf(Objects.nonNull(field.getValidation().getMinLength()), imports, ImportConstants.Jakarta.SIZE);
                ImportCommon.addIf(Objects.nonNull(field.getValidation().getMaxLength()), imports, ImportConstants.Jakarta.SIZE);
                ImportCommon.addIf(Objects.nonNull(field.getValidation().getPattern()), imports, ImportConstants.Jakarta.PATTERN);
                break;
            case INTEGER:
            case LONG:
            case BIG_INTEGER:
                ImportCommon.addIf(Objects.nonNull(field.getValidation().getMin()), imports, ImportConstants.Jakarta.MIN);
                ImportCommon.addIf(Objects.nonNull(field.getValidation().getMax()), imports, ImportConstants.Jakarta.MAX);
                break;
            case DOUBLE:
            case FLOAT:
            case BIG_DECIMAL:
                ImportCommon.addIf(Objects.nonNull(field.getValidation().getMin()), imports, ImportConstants.Jakarta.DECIMAL_MIN);
                ImportCommon.addIf(Objects.nonNull(field.getValidation().getMax()), imports, ImportConstants.Jakarta.DECIMAL_MAX);
                break;
            default:
                break;

        }
        return imports;
    }

    /**
     * Generates a string of import statements for the generated enums, helper entities for JSON fields and transfer objects,
     * if any.
     * 
     * @param modelDefinition      the model definition containing the class name, table name, and field definitions
     * @param outputDir            the directory where the generated code will be written
     * @param importJsonFields     whether to include the helper entities for JSON fields
     * @param target               the target of the generated code
     * @param packageConfiguration the package configuration for the project
     * @return A string containing the necessary import statements for the generated enums, helper entities for JSON fields and
     *         transfer objects.
     */
    public static String computeEnumsAndHelperEntitiesImport(final ModelDefinition modelDefinition, final String outputDir,
            final boolean importJsonFields, final TransferObjectTarget target, final PackageConfiguration packageConfiguration) {

        final Set<String> imports = new LinkedHashSet<>();
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        if (!FieldUtils.isAnyFieldEnum(modelDefinition.getFields()) && !FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            return "";
        }

        imports.addAll(EnumImports.computeEnumImports(modelDefinition, packagePath, packageConfiguration));

        if (importJsonFields) {
            final List<FieldDefinition> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields());
            jsonFields.stream()
                    .map(FieldUtils::extractJsonInnerElementType)
                    .forEach(fieldName -> {
                        switch (target) {
                            case GRAPHQL:
                                imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperGraphqlTransferObjectPackage(packagePath, packageConfiguration), String.format("%sTO", fieldName))));
                                break;
                            case REST:
                                imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperRestTransferObjectPackage(packagePath, packageConfiguration), String.format("%sTO", fieldName))));
                                break;
                            default:
                                imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperEntityPackage(packagePath, packageConfiguration), fieldName)));
                                break;
                        }
                    });
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

}
