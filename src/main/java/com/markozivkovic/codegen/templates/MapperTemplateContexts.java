package com.markozivkovic.codegen.templates;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.markozivkovic.codegen.constants.TemplateContextConstants;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.models.PackageConfiguration;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.ModelNameUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;

public class MapperTemplateContexts {

    private MapperTemplateContexts() {}

    /**
     * Computes a template context for a mapper class of a model definition.
     * 
     * @param modelDefinition      the model definition containing the class and field details
     * @param packagePath          the package path of the directory where the generated class will be written
     * @param swagger              indicates if the mapper is for Swagger models
     * @param isGraphQl            indicates if the mapper is for GraphQL or REST
     * @param packageConfiguration the package configuration for the project
     * @return a template context for the mapper class
     */
    public static Map<String, Object> computeMapperContext(final ModelDefinition modelDefinition, final String packagePath,
            final boolean swagger, final boolean isGraphQl, final PackageConfiguration packageConfiguration) {

        final String strippedModelName = ModelNameUtils.stripSuffix(modelDefinition.getName());
        final String mapperName = isGraphQl ? String.format("%sGraphQLMapper", strippedModelName) : String.format("%sRestMapper", strippedModelName);
        final String transferObjectName = String.format("%sTO", strippedModelName);
        final String modelImport = String.format(IMPORT, PackageUtils.join(PackageUtils.computeEntityPackage(packagePath, packageConfiguration), modelDefinition.getName()));
        final String transferObjectImport;
        
        if (isGraphQl) {
            transferObjectImport = String.format(
                IMPORT, PackageUtils.join(PackageUtils.computeGraphqlTransferObjectPackage(packagePath, packageConfiguration), transferObjectName)
            );
        } else {
            transferObjectImport = String.format(
                IMPORT, PackageUtils.join(PackageUtils.computeRestTransferObjectPackage(packagePath, packageConfiguration), transferObjectName)
            );
        }

        final List<FieldDefinition> jsonFields = FieldUtils.extractJsonFields(modelDefinition.getFields());
        final List<FieldDefinition> relationFields = FieldUtils.extractRelationFields(modelDefinition.getFields());
        final String helperMapperImports = jsonFields.stream()
                .map(FieldUtils::extractJsonFieldName)
                .map(field -> {
                    
                    final String resolvedPackage = isGraphQl ?
                            PackageUtils.join(PackageUtils.computeHelperGraphQlMapperPackage(packagePath, packageConfiguration), String.format("%sGraphQLMapper", field)) :
                            PackageUtils.join(PackageUtils.computeHelperRestMapperPackage(packagePath, packageConfiguration), String.format("%sRestMapper", field));
                    return String.format(IMPORT, resolvedPackage);
                })
                .collect(Collectors.joining(", "));

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_IMPORT, modelImport);
        context.put(TemplateContextConstants.TRANSFER_OBJECT_IMPORT, transferObjectImport);
        
        if (StringUtils.isNotBlank(helperMapperImports)) {
            context.put(TemplateContextConstants.HELPER_MAPPER_IMPORTS, helperMapperImports);
        }
        
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.MAPPER_NAME, mapperName);
        context.put(TemplateContextConstants.TRANSFER_OBJECT_NAME, transferObjectName);
        context.put(TemplateContextConstants.SWAGGER, swagger);
        if (swagger) {
            context.put(TemplateContextConstants.SWAGGER_MODEL, ModelNameUtils.stripSuffix(modelDefinition.getName()));
            final String resolvedPackagePath = PackageUtils.join(
                    PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, StringUtils.uncapitalize(strippedModelName)), strippedModelName
            );
            context.put(TemplateContextConstants.GENERATED_MODEL_IMPORT, String.format(IMPORT, resolvedPackagePath));
        }
        
        if (!relationFields.isEmpty() || !jsonFields.isEmpty()) {
            final String mapperParameters = Stream.concat(relationFields.stream(), jsonFields.stream())
                    .map(field -> FieldUtils.isJsonField(field) ? FieldUtils.extractJsonFieldName(field) : ModelNameUtils.stripSuffix(field.getType()))
                    .map(field -> isGraphQl ? String.format("%sGraphQLMapper.class", field) : String.format("%sRestMapper.class", field))
                    .distinct()
                    .collect(Collectors.joining(", "));
            context.put(TemplateContextConstants.PARAMETERS, mapperParameters);
        }

        return context;
    }

    /**
     * Computes a template context for a mapper class of a model definition.
     * 
     * @param parentModel          the parent model definition containing the class and field details
     * @param jsonModel            the json model definition containing the class and field details
     * @param packagePath          the package path of the directory where the generated class will be written
     * @param swagger              indicates if the mapper is for Swagger models
     * @param isGraphQl            indicates if the mapper is for GraphQL or REST
     * @param packageConfiguration the package configuration for the project
     * @return a template context for the mapper class 
     */
    public static Map<String, Object> computeHelperMapperContext(final ModelDefinition parentModel, final ModelDefinition jsonModel,
            final String packagePath, final boolean swagger, final boolean isGraphQl, final PackageConfiguration packageConfiguration) {

        final String mapperName = isGraphQl ? String.format("%sGraphQLMapper", ModelNameUtils.stripSuffix(jsonModel.getName())) :
                String.format("%sRestMapper", ModelNameUtils.stripSuffix(jsonModel.getName()));
        final String transferObjectName = String.format("%sTO", ModelNameUtils.stripSuffix(jsonModel.getName()));
        final String modelImport = String.format(IMPORT, PackageUtils.join(PackageUtils.computeHelperEntityPackage(packagePath, packageConfiguration), jsonModel.getName()));
        final String transferObjectImport;
        if (isGraphQl) {
            transferObjectImport = String.format(
                IMPORT, PackageUtils.join(PackageUtils.computeHelperGraphqlTransferObjectPackage(packagePath, packageConfiguration), transferObjectName)
            );
        } else {
            transferObjectImport = String.format(
                IMPORT, PackageUtils.join(PackageUtils.computeHelperRestTransferObjectPackage(packagePath, packageConfiguration), transferObjectName)
            );
        }
        
        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.MODEL_IMPORT, modelImport);
        context.put(TemplateContextConstants.TRANSFER_OBJECT_IMPORT, transferObjectImport);
        context.put(TemplateContextConstants.MODEL_NAME, jsonModel.getName());
        context.put(TemplateContextConstants.MAPPER_NAME, mapperName);
        context.put(TemplateContextConstants.TRANSFER_OBJECT_NAME, transferObjectName);

        if (swagger) {
            final String resolvedPackagePath = PackageUtils.join(
                    PackageUtils.computeGeneratedModelPackage(packagePath, packageConfiguration, StringUtils.uncapitalize(ModelNameUtils.stripSuffix(parentModel.getName()))),
                    ModelNameUtils.stripSuffix(jsonModel.getName())
            );
            context.put(TemplateContextConstants.GENERATED_MODEL_IMPORT, resolvedPackagePath);
        }

        context.put(TemplateContextConstants.SWAGGER, false);
        context.put(TemplateContextConstants.SWAGGER_MODEL, ModelNameUtils.stripSuffix(jsonModel.getName()));
        context.put(TemplateContextConstants.GENERATE_ALL_HELPER_METHODS, swagger);

        return context;
    }
    
}
