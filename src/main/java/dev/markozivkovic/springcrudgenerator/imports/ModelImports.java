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
import java.util.stream.Stream;

import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.constants.RelationTypesConstants;
import dev.markozivkovic.springcrudgenerator.enums.SpecialType;
import dev.markozivkovic.springcrudgenerator.imports.common.ImportCommon;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.models.IdDefinition.IdStrategyEnum;
import dev.markozivkovic.springcrudgenerator.utils.AuditUtils;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.StringUtils;

public class ModelImports {
    
    private ModelImports() {}

    /**
     * Generates a string of import statements based on the fields present in the given model definition.
     *
     * @param modelDefinition The model definition containing field information used to determine necessary imports.
     * @param importObjects   Whether to include the java.util.Objects import.
     * @param importAuditing  Whether to include the auditing imports
     * @return A string containing the necessary import statements for the model.
     */
    public static String getBaseImport(final ModelDefinition modelDefinition, final boolean importObjects, final boolean importAuditing) {
        
        final StringBuilder sb = new StringBuilder();

        final List<FieldDefinition> fields = modelDefinition.getFields();
        final Set<String> imports = new LinkedHashSet<>();

        if (FieldUtils.isAnyFieldSimpleCollection(fields)) {
            final List<FieldDefinition> simpleCollectionFields = FieldUtils.extractSimpleCollectionFields(modelDefinition.getFields());
            simpleCollectionFields.forEach(field -> {
                if (SpecialType.isListType(field.getType())) {
                    imports.add(ImportConstants.Java.LIST);
                    imports.add(ImportConstants.Java.ARRAY_LIST);
                }

                if (SpecialType.isSetType(field.getType())) {
                    imports.add(ImportConstants.Java.SET);
                    imports.add(ImportConstants.Java.HASH_SET);
                }
            });
        }

        ImportCommon.addIf(FieldUtils.isAnyFieldBigDecimal(fields), imports, ImportConstants.Java.BIG_DECIMAL);
        ImportCommon.addIf(FieldUtils.isAnyFieldBigInteger(fields), imports, ImportConstants.Java.BIG_INTEGER);
        ImportCommon.addIf(FieldUtils.isAnyFieldLocalDate(fields), imports, ImportConstants.Java.LOCAL_DATE);
        ImportCommon.addIf(FieldUtils.isAnyFieldLocalDateTime(fields), imports, ImportConstants.Java.LOCAL_DATE_TIME);
        ImportCommon.addIf(importObjects, imports, ImportConstants.Java.OBJECTS);
        
        if (modelDefinition.getAudit() != null) {
            ImportCommon.addIf(
                importAuditing && Objects.nonNull(modelDefinition.getAudit()) && modelDefinition.getAudit().isEnabled(),
                imports,
                AuditUtils.resolveAuditingImport(modelDefinition.getAudit().getType())
            );
        }
        ImportCommon.addIf(FieldUtils.isAnyFieldUUID(fields), imports, ImportConstants.Java.UUID);
        
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
     * Generates a string of import statements for the base jakarta persistence annotations.
     * 
     * @param modelDefinition   the model definition containing the class name, table name, and field definitions
     * @param optimisticLocking whether to include the version field
     * @param importSequence    whether to include the sequence generator
     * @return A string containing the necessary import statements for the base jakarta persistence annotations.
     */
    public static String computeJakartaImports(final ModelDefinition modelDefinition, final Boolean optimisticLocking,
                final Boolean importSequence, final Boolean openInViewEnabled) {

        final Set<String> imports = new LinkedHashSet<>();
        final List<FieldDefinition> fields = modelDefinition.getFields();
        final List<String> relations = FieldUtils.extractRelationTypes(fields);
        final FieldDefinition idField = FieldUtils.extractIdField(fields);

        ImportCommon.addIf(IdStrategyEnum.TABLE.equals(idField.getId().getStrategy()), imports, ImportConstants.Jakarta.TABLE_GENERATOR);
        ImportCommon.addIf(IdStrategyEnum.SEQUENCE.equals(idField.getId().getStrategy()) || importSequence, imports, ImportConstants.Jakarta.SEQUENCE_GENERATOR);

        imports.addAll(Set.of(
            ImportConstants.Jakarta.ENTITY, ImportConstants.Jakarta.GENERATED_VALUE, ImportConstants.Jakarta.GENERATION_TYPE,
            ImportConstants.Jakarta.ID, ImportConstants.Jakarta.TABLE
        ));
        
        if (FieldUtils.isAnyFieldEnum(fields)) {
            imports.add(ImportConstants.Jakarta.ENUM_TYPE);
            imports.add(ImportConstants.Jakarta.ENUMERATED);
        }

        final boolean hasAnyFieldColumn = FieldUtils.isAnyFieldJson(fields) || fields.stream()
                .anyMatch(field -> Objects.nonNull(field.getColumn()));
        final boolean isAuditingEnabled = Objects.nonNull(modelDefinition.getAudit()) && modelDefinition.getAudit().isEnabled();
        final boolean isAnyFieldSimpleCollection = FieldUtils.isAnyFieldSimpleCollection(fields);
        final boolean hasLazyFields = FieldUtils.hasLazyFetchField(fields) && !openInViewEnabled;
        
        ImportCommon.addIf(!relations.isEmpty() || isAnyFieldSimpleCollection, imports, ImportConstants.Jakarta.JOIN_COLUMN);
        ImportCommon.addIf(relations.contains(RelationTypesConstants.MANY_TO_MANY), imports, ImportConstants.Jakarta.JOIN_TABLE);
        ImportCommon.addIf(FieldUtils.isAnyRelationManyToMany(fields), imports, ImportConstants.Jakarta.MANY_TO_MANY);
        ImportCommon.addIf(FieldUtils.isAnyRelationManyToOne(fields), imports, ImportConstants.Jakarta.MANY_TO_ONE);
        ImportCommon.addIf(FieldUtils.isAnyRelationOneToMany(fields), imports, ImportConstants.Jakarta.ONE_TO_MANY);
        ImportCommon.addIf(FieldUtils.isAnyRelationOneToOne(fields), imports, ImportConstants.Jakarta.ONE_TO_ONE);
        ImportCommon.addIf(FieldUtils.isFetchTypeDefined(fields), imports, ImportConstants.Jakarta.FETCH_TYPE);
        ImportCommon.addIf(FieldUtils.isCascadeTypeDefined(fields), imports, ImportConstants.Jakarta.CASCADE_TYPE);
        ImportCommon.addIf(optimisticLocking, imports, ImportConstants.Jakarta.VERSION);
        ImportCommon.addIf(hasAnyFieldColumn || isAuditingEnabled, imports, ImportConstants.Jakarta.COLUMN);
        ImportCommon.addIf(isAuditingEnabled, imports, ImportConstants.Jakarta.ENTITY_LISTENERS);
        ImportCommon.addIf(isAnyFieldSimpleCollection, imports, ImportConstants.Jakarta.ELEMENT_COLLECTION);
        ImportCommon.addIf(isAnyFieldSimpleCollection, imports, ImportConstants.Jakarta.COLLECTION_TABLE);
        ImportCommon.addIf(FieldUtils.isAnyFieldSimpleListType(fields), imports, ImportConstants.Jakarta.ORDER_COLUMN);
        ImportCommon.addIf(hasLazyFields, imports, ImportConstants.Jakarta.NAMED_ATTRIBUTE_NODE);
        ImportCommon.addIf(hasLazyFields, imports, ImportConstants.Jakarta.NAMED_ENTITY_GRAPH);

        final String jakartaImports = imports.stream()
                  .map(imp -> String.format(IMPORT, imp))
                  .sorted()
                  .collect(Collectors.joining());

        final Set<String> orgImports = new LinkedHashSet<>();
        ImportCommon.addIf(isAuditingEnabled, orgImports, ImportConstants.SpringData.AUDITING_ENTITY_LISTENER);
        ImportCommon.addIf(isAuditingEnabled, orgImports, ImportConstants.SpringData.CREATED_DATE);
        ImportCommon.addIf(isAuditingEnabled, orgImports, ImportConstants.SpringData.LAST_MODIFIED_DATE);
        
        if (!FieldUtils.isAnyFieldJson(fields)) {
            if (orgImports.isEmpty()) {
                return jakartaImports;
            }

            final String orgImportsFormatted = orgImports.stream()
                    .map(imp -> String.format(IMPORT, imp))
                    .sorted()
                    .collect(Collectors.joining());

            return String.format("%s%n%s", jakartaImports, orgImportsFormatted);
        }

        final String hibernateImports = Stream.concat(
                    Set.of(ImportConstants.HibernateAnnotation.JDBC_TYPE_CODE, ImportConstants.HibernateAnnotation.SQL_TYPES).stream(),
                    orgImports.stream()
                )
                .map(imp -> String.format(IMPORT, imp))
                .sorted()
                .collect(Collectors.joining());

        return String.format("%s%n%s", jakartaImports, hibernateImports);
    }

    /**
     * Generates a string of import statements for the generated enums, helper entities for JSON fields and transfer objects,
     * if any.
     * 
     * @param modelDefinition      the model definition containing the class name, table name, and field definitions
     * @param outputDir            the directory where the generated code will be written
     * @param packageConfiguration the package configuration for the project
     * @return A string containing the necessary import statements for the generated enums, helper entities for JSON fields and
     *         transfer objects.
     */
    public static String computeEnumsAndHelperEntitiesImport(final ModelDefinition modelDefinition, final String outputDir,
                final PackageConfiguration packageConfiguration) {

        final Set<String> imports = new LinkedHashSet<>();
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        if (!FieldUtils.isAnyFieldEnum(modelDefinition.getFields()) && !FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            return "";
        }

        imports.addAll(EnumImports.computeEnumImports(modelDefinition, packagePath, packageConfiguration));

        final List<FieldDefinition> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields());
        jsonFields.stream()
                .map(FieldUtils::extractJsonFieldName)
                .forEach(fieldName -> {
                    imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperEntityPackage(packagePath, packageConfiguration), fieldName)));
                });

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

}
