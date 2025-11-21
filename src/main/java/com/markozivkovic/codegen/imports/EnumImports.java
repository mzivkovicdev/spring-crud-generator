package com.markozivkovic.codegen.imports;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;

public class EnumImports {

    private EnumImports() {}

    /**
     * Computes the necessary imports for the given model definition, including the enums if any exist.
     *
     * @param modelDefinition      the model definition containing the class name, table name, and field definitions
     * @param packagePath          the package path where the generated code will be written
     * @param packageConfiguration the package configuration for the project
     * @return A set of strings containing the necessary import statements for the given model.
     */
    public static Set<String> computeEnumImports(final ModelDefinition modelDefinition, final String packagePath,
                final PackageConfiguration packageConfiguration) {
        
        final List<FieldDefinition> enumFields = FieldUtils.extractEnumFields(modelDefinition.getFields());
        final Set<String> imports = new LinkedHashSet<>();

        enumFields.forEach(enumField -> {
            
            final String enumName;
            if (!enumField.getName().endsWith("Enum")) {
                enumName = String.format("%sEnum", StringUtils.capitalize(enumField.getName()));
            } else {
                enumName = StringUtils.capitalize(enumField.getName());
            }

            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeEnumPackage(packagePath, packageConfiguration), enumName)));
        });

        return imports;
    }
}
