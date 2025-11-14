package com.markozivkovic.codegen.imports;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.constants.GeneratorConstants.DefaultPackageLayout;
import com.markozivkovic.codegen.generators.TransferObjectGenerator.TransferObjectTarget;
import com.markozivkovic.codegen.generators.TransferObjectGenerator.TransferObjectType;
import com.markozivkovic.codegen.imports.common.ImportCommon;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;

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
        
        final boolean hasLists = FieldUtils.isAnyRelationOneToMany(fields) ||
                FieldUtils.isAnyRelationManyToMany(fields);

        ImportCommon.addIf(hasLists, imports, ImportConstants.Java.LIST);

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
     * Computes the necessary imports for the given model definition, including imports for the types of its fields,
     * as well as imports for the types of its relations, if any.
     *
     * @param modelDefinition the model definition containing field information used to determine necessary imports.
     * @param entities        the list of all model definitions, used to determine the necessary imports for relations.
     * @param realtionIds     whether to include the imports for the relation IDs.
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
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Generates a string of import statements for the generated enums, helper entities for JSON fields and transfer objects,
     * if any.
     * 
     * @param modelDefinition  the model definition containing the class name, table name, and field definitions
     * @param outputDir        the directory where the generated code will be written
     * @param importJsonFields whether to include the helper entities for JSON fields
     * @param restTOs          whether to include the REST transfer objects
     * @param graphqlTOs       whether to include the GraphQL transfer objects
     * @return A string containing the necessary import statements for the generated enums, helper entities for JSON fields and
     *         transfer objects.
     */
    public static String computeEnumsAndHelperEntitiesImport(final ModelDefinition modelDefinition, final String outputDir,
            final boolean importJsonFields, final TransferObjectTarget target) {

        final Set<String> imports = new LinkedHashSet<>();
        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);

        if (!FieldUtils.isAnyFieldEnum(modelDefinition.getFields()) && !FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            return "";
        }

        imports.addAll(EnumImports.computeEnumImports(modelDefinition, outputDir, packagePath));

        if (importJsonFields) {
            final List<FieldDefinition> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields());
            jsonFields.stream()
                    .map(FieldUtils::extractJsonFieldName)
                    .forEach(fieldName -> {
                        switch (target) {
                            case GRAPHQL:
                                imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.GRAPHQL, DefaultPackageLayout.HELPERS, String.format("%sTO", fieldName))));
                                break;
                            case REST:
                                imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.REST, DefaultPackageLayout.HELPERS, String.format("%sTO", fieldName))));
                                break;
                            default:
                                imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MODELS, DefaultPackageLayout.HELPERS, fieldName)));
                                break;
                        }
                    });
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

}
