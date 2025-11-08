package com.markozivkovic.codegen.imports;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.markozivkovic.codegen.constants.ImportConstants;
import com.markozivkovic.codegen.imports.common.ImportCommon;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
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

}
