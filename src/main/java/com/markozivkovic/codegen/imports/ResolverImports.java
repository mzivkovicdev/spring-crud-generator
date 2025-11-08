package com.markozivkovic.codegen.imports;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.constants.GeneratorConstants.DefaultPackageLayout;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;

public class ResolverImports {
    
    private ResolverImports() {}

    /**
     * Computes the necessary imports for the given model definition, including UUID if any model has a UUID as its ID.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeResolverBaseImports(final ModelDefinition modelDefinition) {

        final Set<String> imports = new LinkedHashSet<>();

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        if (FieldUtils.isIdFieldUUID(idField)) {
            imports.add(String.format(IMPORT, ImportConstants.Java.UUID));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

    /**
     * Computes the necessary imports for the given model definition, including the graphql mappers, graphql mappers helpers, and the page transfer object.
     *
     * @param modelDefinition the model definition containing the class name, table name, and field definitions
     * @param outputDir       the directory where the generated code will be written
     * @return A string containing the necessary import statements for the given model.
     */
    public static String computeGraphQlResolverImports(final ModelDefinition modelDefinition, final String outputDir) {
        
        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String modelWithoutSuffix = ModelNameUtils.stripSuffix(modelDefinition.getName());

        if (FieldUtils.isAnyFieldJson(modelDefinition.getFields())) {
            modelDefinition.getFields().stream()
                .filter(field -> FieldUtils.isJsonField(field))
                .map(field -> FieldUtils.extractJsonFieldName(field))
                .forEach(jsonField -> {
                    imports.add(String.format(
                        IMPORT,
                        PackageUtils.join(packagePath, DefaultPackageLayout.GRAPHQL, DefaultPackageLayout.HELPERS, String.format("%sGraphQLMapper", jsonField))
                    ));
                });
        }
        
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MODELS, modelDefinition.getName())));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.SERVICES, String.format("%sService", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.GRAPHQL, String.format("%sTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.GRAPHQL, String.format("%sCreateTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, DefaultPackageLayout.GRAPHQL, String.format("%sUpdateTO", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.MAPPERS, DefaultPackageLayout.GRAPHQL, String.format("%sGraphQLMapper", modelWithoutSuffix))));
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.TRANSFEROBJECTS, "PageTO")));

        if (!FieldUtils.extractRelationTypes(modelDefinition.getFields()).isEmpty()) {
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.BUSINESS_SERVICES, String.format("%sBusinessService", modelWithoutSuffix))));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

}
