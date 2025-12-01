package com.markozivkovic.codegen.validators;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.enums.BasicType;
import com.markozivkovic.codegen.enums.SpecialType;
import com.markozivkovic.codegen.models.CrudConfiguration;
import com.markozivkovic.codegen.models.CrudSpecification;
import com.markozivkovic.codegen.models.FieldDefinition;
import com.markozivkovic.codegen.models.ModelDefinition;
import com.markozivkovic.codegen.utils.ContainerUtils;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.StringUtils;

public class SpecificationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpecificationValidator.class);
    private static final int MIN_SUPPORTED_JAVA = 17;
    private static final int MAX_SUPPORTED_JAVA = 25;

    private SpecificationValidator() {}

    /**
     * Validates a CRUD specification.
     * 
     * Checks if the CRUD specification, configuration and entities are not null.
     * Then, for each entity, it checks if the model name and storage name are not null or empty,
     * and if the model has at least one field defined. Then, for each field, it checks if the field name is
     * not null or empty and if it is not duplicated. Finally, it validates each field type, enum values and JSON
     * type if applicable.
     * 
     * @param specification the CRUD specification to validate
     * @throws IllegalArgumentException if the CRUD specification is invalid
     */
    public static void validate(final CrudSpecification specification) {
        
        if (Objects.isNull(specification) || Objects.isNull(specification.getConfiguration())
                    || ContainerUtils.isEmpty(specification.getEntities())) {
            throw new IllegalArgumentException("CRUD specification, configuration and entities must not be null");
        }

        validateJavaVersion(specification.getConfiguration());

        DockerConfigurationValidator.validate(specification.getConfiguration().getDocker());
        CacheConfigurationValidator.validate(specification.getConfiguration().getCache());

        specification.getEntities().forEach(model -> validateModel(model, specification.getEntities()));
    }

    /**
     * Validates the Java version set in the CRUD configuration.
     * 
     * If the Java version is not set, it defaults to the minimum supported version.
     * If the set Java version is less than the minimum supported version, it throws an {@link IllegalArgumentException}.
     * If the set Java version is greater than the maximum supported version, it throws an {@link IllegalArgumentException}.
     * 
     * @param configuration the CRUD configuration containing the Java version to validate
     * @throws IllegalArgumentException if the set Java version is not supported
     */
    private static void validateJavaVersion(final CrudConfiguration configuration) {
        
        if (Objects.isNull(configuration.getJavaVersion())) {
            LOGGER.info(
                String.format(
                    "Java version is not set, defaulting to Java %d", MIN_SUPPORTED_JAVA
                )
            );
            return;
        }

        if (configuration.getJavaVersion() < 17) {
            throw new IllegalArgumentException(
                String.format(
                    "Java version %d is not supported. Minimum supported version is %d.",
                    configuration.getJavaVersion(), MIN_SUPPORTED_JAVA
                )
            );
        }

        if (configuration.getJavaVersion() > 25) {
            throw new IllegalArgumentException(
                String.format(
                    "Java version %d is not supported. Maximum supported version is %d.",
                    configuration.getJavaVersion(), MAX_SUPPORTED_JAVA
                )
            );
        }
    }

    /**
     * Validates a model definition.
     *
     * Checks if the model name and storage name are not null or empty, and if the model has at least one field defined.
     * Then, for each field, it checks if the field name is not null or empty and if it is not duplicated.
     * Finally, it validates each field type, enum values and JSON type if applicable.
     *
     * @param model  the model definition to validate
     * @param models the set of names of all models in the CRUD specification
     * @throws IllegalArgumentException if the model definition is invalid
     */
    private static void validateModel(final ModelDefinition model, final List<ModelDefinition> models) {

        validateModelBasics(model, models);

        final Set<String> modelNames = models.stream()
                .map(ModelDefinition::getName)
                .collect(Collectors.toSet());

        final Set<String> fieldNames = new HashSet<>();

        model.getFields().forEach(field -> {
            if (StringUtils.isBlank(field.getName())) {
                throw new IllegalArgumentException(
                    String.format("Field name in model %s must not be null or empty", model.getName())
                );
            }

            if (!fieldNames.add(field.getName())) {
                throw new IllegalArgumentException(
                    String.format("Field name %s in model %s is duplicated", field.getName(), model.getName())
                );
            }

            validateFieldType(model, field, modelNames);
            validateEnumValues(model, field);
            validateJsonType(model, field, modelNames);
        });
    }

    /**
     * Validates the basic properties of a model definition.
     * 
     * Checks if the model name is not null or empty, and if the model has at least one field defined.
     * Then, for non-JSON models, it checks if the storage name is not null or empty, and if it matches the lower_snake_case
     * pattern. Finally, it checks if the model has exactly one id field defined.
     * 
     * @param model  the model definition to validate
     * @param models the set of names of all models in the CRUd specification
     * @throws IllegalArgumentException if the model definition is invalid
     */
    private static void validateModelBasics(final ModelDefinition model, final List<ModelDefinition> models) {

        if (StringUtils.isBlank(model.getName())) {
            throw new IllegalArgumentException("Model name must not be null or empty");
        }
        
        final boolean usedAsJson = FieldUtils.isModelUsedAsJsonField(model, models);
        if (!usedAsJson && StringUtils.isBlank(model.getStorageName())) {
            throw new IllegalArgumentException("Model storage name must not be null or empty");
        }

        if (StringUtils.isNotBlank(model.getStorageName()) && !model.getStorageName().matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException(
                String.format("Invalid storageName '%s'. Table names should be lower_snake_case.", model.getStorageName())
            );
        }
        
        if (ContainerUtils.isEmpty(model.getFields())) {
            throw new IllegalArgumentException(String.format("Model %s must have at least one field defined", model.getName()));
        }
        
        if (!usedAsJson) {
            final long idCount = model.getFields().stream()
                    .filter(field -> Objects.nonNull(field.getId()))
                    .count();

            if (idCount == 0) {
                throw new IllegalArgumentException(String.format("Model %s must have id field defined", model.getName()));
            }

            if (idCount > 1) {
                throw new IllegalArgumentException(String.format("Model %s must have only one id field defined", model.getName()));
            }
        }
    }

    /**
     * Validates the inner type of a JSON field.
     *
     * Checks if the inner type of a JSON field is a basic type or a reference to another model.
     * If the inner type is neither a basic type nor a reference to another model, an exception is thrown.
     *
     * @param model      the model definition containing the JSON field
     * @param field      the JSON field definition to validate
     * @param modelNames the set of names of all models in the CRUd specification
     * @throws IllegalArgumentException if the inner type of the JSON field is invalid
     */
    private static void validateJsonType(final ModelDefinition model, final FieldDefinition field, final Set<String> modelNames) {
        
        if (!SpecialType.isJsonType(field.getType())) return;

        final String inner;
        try {
            inner = FieldUtils.extractJsonFieldName(field);
        } catch (final IllegalStateException e) {
            throw new IllegalArgumentException(
                String.format(
                    "JSON field %s.%s has invalid inner type. Please specify a valid inner type.",
                    model.getName(), field.getName()
                )
            );
        }

        final boolean isBasicType = BasicType.isBasicType(inner);
        final boolean innerModel = modelNames.contains(inner);

        if (!isBasicType && !innerModel) {
            throw new IllegalArgumentException(
                String.format(
                    "Inner type %s of JSON field %s.%s is invalid. It must be a basic type [%s] or reference to another model.",
                    inner, model.getName(), field.getName(), BasicType.getSupportedValues()
                )
            );
        }
    }

    /**
     * Validates the enum values of a field in a model definition.
     * 
     * If the field is of enum type, this method checks if the field has any enum values defined.
     * If no enum values are defined, it throws an exception.
     * 
     * @param model the model definition
     * @param field the field definition
     */
    private static void validateEnumValues(final ModelDefinition model, final FieldDefinition field) {

        if (SpecialType.isEnumType(field.getType())) {
            if (ContainerUtils.isEmpty(field.getValues())) {
                throw new IllegalArgumentException(
                    String.format(
                        "Field %s in model %s is of enum type but has no enum values defined. Please define enum values.",
                        field.getName(), model.getName()
                    )
                );
            }
        }
    }

    /**
     * Validates the type of a field in a model definition.
     * 
     * Checks if the field type is null or empty, and if it is not a basic type, enum type, json type or reference to another model.
     * 
     * @param model      the model definition containing the field
     * @param field      the field definition to validate
     * @param modelNames the set of names of all models in the CRUd specification
     * @throws IllegalArgumentException if the field type is invalid
     */
    private static void validateFieldType(final ModelDefinition model, final FieldDefinition field, final Set<String> modelNames) {
        
        if (StringUtils.isBlank(field.getType())) {
            throw new IllegalArgumentException(
                String.format("Field type for field %s in model %s must not be null or empty", field.getName(), model.getName())
            );
        }
        
        final String type = field.getType();
        final boolean isBasicType = BasicType.isBasicType(type);
        final boolean isEnumType = SpecialType.isEnumType(type);
        final boolean isJsonType = SpecialType.isJsonType(type);
        final boolean modelReference = modelNames.contains(type);

        if (!isBasicType && !isEnumType && !isJsonType && !modelReference) {
            throw new IllegalArgumentException(
                String.format(
                    "Field type %s for field %s in model %s is invalid. It must be a basic type [%s], special type [%s] or reference to another model.",
                    type, field.getName(), model.getName(), BasicType.getSupportedValues(), SpecialType.getSupportedValues()
                )
            );
        }
    }
    
}
