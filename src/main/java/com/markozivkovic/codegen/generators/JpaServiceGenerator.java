package com.markozivkovic.codegen.generators;

import static com.markozivkovic.codegen.constants.JPAConstants.SPRING_DATA_PACKAGE_DOMAIN_PAGE;
import static com.markozivkovic.codegen.constants.JPAConstants.SPRING_DATA_PACKAGE_DOMAIN_PAGE_REQUEST;
import static com.markozivkovic.codegen.constants.JavaConstants.IMPORT;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_TIME_LOCAL_DATE;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_TIME_LOCAL_DATE_TIME;
import static com.markozivkovic.codegen.constants.JavaConstants.JAVA_UTIL_UUID;
import static com.markozivkovic.codegen.constants.JavaConstants.PACKAGE;
import static com.markozivkovic.codegen.constants.LoggerConstants.SL4J_LOGGER;
import static com.markozivkovic.codegen.constants.LoggerConstants.SL4J_LOGGER_FACTORY;
import static com.markozivkovic.codegen.constants.SpringConstants.SPRING_FRAMEWORK_STEREOTYPE_SERVICE;
import static com.markozivkovic.codegen.constants.TransactionConstants.SPRING_FRAMEWORK_TRANSACTION_ANNOTATION_TRANSACTIONAL;
import static com.markozivkovic.codegen.constants.TransactionConstants.TRANSACTIONAL_ANNOTATION;

import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.utils.FieldUtils;
import com.markozivkovic.codegen.utils.FileWriterUtils;
import com.markozivkovic.codegen.utils.PackageUtils;
import com.markozivkovic.codegen.utils.StringUtils;

public class JpaServiceGenerator implements CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaServiceGenerator.class);

    private  static final String SERVICES = "services";
    private static final String REPOSITORIES_PACKAGE = ".repositories";
    private static final String MODELS_PACKAGE = ".models";
    private static final String SERVICES_PACKAGE = "." + SERVICES;
    
    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        
        LOGGER.info("Generating JPA service for model: {}", modelDefinition.getName());
        
        final boolean hasIdField = FieldUtils.isAnyFieldId(modelDefinition.getFields());
        
        if (!hasIdField) {
            LOGGER.warn("Model {} does not have an ID field. Skipping service generation.", modelDefinition.getName());
            return;
        }

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String className = modelDefinition.getName() + "Service";
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());
        final List<FieldDefinition> fields = modelDefinition.getFields();

        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(PACKAGE, packagePath + SERVICES_PACKAGE));

        if (FieldUtils.isAnyFieldLocalDate(fields)) {
            sb.append(String.format(IMPORT, JAVA_TIME_LOCAL_DATE));
        }

        if (FieldUtils.isAnyFieldLocalDateTime(fields)) {
            sb.append(String.format(IMPORT, JAVA_TIME_LOCAL_DATE_TIME));
        }

        if (FieldUtils.isIdFieldUUID(idField)) {
            sb.append(String.format(IMPORT, JAVA_UTIL_UUID))
                    .append("\n");
        }
        
        sb.append(String.format(IMPORT, SL4J_LOGGER))
                .append(String.format(IMPORT, SL4J_LOGGER_FACTORY))
                .append(String.format(IMPORT, SPRING_DATA_PACKAGE_DOMAIN_PAGE))
                .append(String.format(IMPORT, SPRING_DATA_PACKAGE_DOMAIN_PAGE_REQUEST))
                .append(String.format(IMPORT, SPRING_FRAMEWORK_STEREOTYPE_SERVICE))
                .append(String.format(IMPORT, SPRING_FRAMEWORK_TRANSACTION_ANNOTATION_TRANSACTIONAL))
                .append("\n")
                .append(String.format(IMPORT, packagePath + MODELS_PACKAGE + "." + modelDefinition.getName()))
                .append(String.format(IMPORT, packagePath + REPOSITORIES_PACKAGE + "." + modelDefinition.getName() + "Repository"))
                .append("\n")
                .append("@Service\n")
                .append(String.format("public class %s {", className))
                .append("\n\n")
                .append(String.format("    private static final Logger LOGGER = LoggerFactory.getLogger(%s.class);", className))
                .append("\n\n")
                .append(String.format("    private final %s repository;", modelDefinition.getName() + "Repository"))
                .append("\n\n")
                .append(String.format("    public %s(final %s repository) {", className, modelDefinition.getName() + "Repository"))
                .append("\n")
                .append(String.format("        this.repository = repository;"))
                .append("\n")
                .append("    }")
                .append("\n\n");
                
        sb.append(this.generateGetByIdMethod(modelDefinition))
                .append("\n\n")
                .append(this.generateGetAllMethod(modelDefinition))
                .append("\n\n")
                .append(this.generateCreateMethod(modelDefinition))
                .append("\n\n")
                .append(this.generateUpdateByIdMethod(modelDefinition))
                .append("\n\n")
                .append(this.generateDeleteByIdMethod(modelDefinition))
                .append("\n\n");

        sb.append("}");

        FileWriterUtils.writeToFile(outputDir, SERVICES, className, sb.toString());
    }

    /**
     * Generates the getAll method as a string for the given model.
     * 
     * @param modelDefinition The model definition for which the getAll method 
     *                        is to be generated.
     * @return A string representation of the getAll method.
     */
    private String generateGetAllMethod(final ModelDefinition modelDefinition) {
        
        final StringBuilder sb = new StringBuilder();
        final String className = modelDefinition.getName();

        sb.append("    /**\n")
                .append(String.format("     * Get all {@link %s} with pagination by page number and page size.\n", modelDefinition.getName()))
                .append("     *\n")
                .append("     * @param pageNumber The page number.\n")
                .append("     * @param pageSize The page size.\n")
                .append(String.format("     * @return A page of {@link %s}.\n", modelDefinition.getName()))
                .append("     */\n")
                .append(String.format("    public Page<%s> getAll(final Integer pageNumber, final Integer pageSize) {", className))
                .append("\n\n")
                .append(String.format("        return repository.findAll(PageRequest.of(pageNumber, pageSize));"))
                .append("\n")
                .append("    }");
        
        return sb.toString();
    }

    /**
     * Generates the create method as a string for the given model definition.
     * 
     * @param modelDefinition The model definition for which the create method
     *                        is to be generated.
     * @return A string representation of the create method.
     */
    public String generateCreateMethod(final ModelDefinition modelDefinition) {
        
        final StringBuilder sb = new StringBuilder();

        final List<String> inputFields = FieldUtils.generateInputArgsExcludingId(modelDefinition.getFields());
        final List<String> fieldNames = FieldUtils.extractNonIdFieldNames(modelDefinition.getFields());
        final List<String> javadocFields = FieldUtils.extractNonIdFieldForJavadoc(modelDefinition.getFields());

        if (!javadocFields.isEmpty()) {

            sb.append("    /**\n")
                    .append(String.format("     * Creates a new {@link %s}.\n", modelDefinition.getName()))
                    .append("     *\n");
    
            javadocFields.forEach(javadocField -> sb.append(String.format("     * %s\n", javadocField)));
            sb.append(String.format("     * @return the created {@link %s}\n", modelDefinition.getName()));
            sb.append("     */\n");
        }

        sb.append(String.format("    %s", TRANSACTIONAL_ANNOTATION))
                .append("\n")
                .append(String.format(("    public %s create(%s) {"), modelDefinition.getName(), String.join(", ", inputFields)))
                .append("\n\n")
                .append(String.format("        LOGGER.info(\"Creating new %s\");", modelDefinition.getName()))
                .append("\n\n")
                .append(String.format("        return this.repository.saveAndFlush(new %s(%s));", modelDefinition.getName(), String.join(", ", fieldNames)))
                .append("\n")
                .append("    }");
        
        return sb.toString();
    }

    /**
     * Generates the updateById method as a string for the given model definition.
     *
     * @param modelDefinition The model definition for which the updateById method
     *                        is to be generated.
     * @return A string representation of the updateById method.
     */
    public String generateUpdateByIdMethod(final ModelDefinition modelDefinition) {
        
        final StringBuilder sb = new StringBuilder();

        final List<String> inputFields = FieldUtils.generateInputArgs(modelDefinition.getFields());
        final List<String> fieldNamesWithoutId = FieldUtils.extractNonIdFieldNames(modelDefinition.getFields());

        final List<String> javadocFields = FieldUtils.extractFieldForJavadoc(modelDefinition.getFields());

        if (!javadocFields.isEmpty()) {
            
            sb.append("    /**\n")
                    .append(String.format("     * Updates an existing {@link %s}\n", modelDefinition.getName()))
                    .append("     *\n");
    
            javadocFields.forEach(javadocField -> sb.append(String.format("     * %s\n", javadocField)));
            sb.append(String.format("     * @return updated {@link %s}\n", modelDefinition.getName()));
            sb.append("     */\n");
        }

        sb.append(String.format("    %s", TRANSACTIONAL_ANNOTATION))
                .append("\n")
                .append(String.format(("    public %s updateById(%s) {"), modelDefinition.getName(), String.join(", ", inputFields)))
                .append("\n\n")
                .append(String.format("        final %s existing = this.getById(id);", modelDefinition.getName()))
                .append("\n\n");

        IntStream.range(0, fieldNamesWithoutId.size()).forEach(i -> {
            
            final String field = fieldNamesWithoutId.get(i);
            
            if (i == 0) {
                sb.append(String.format("        existing.set%s(%s)", StringUtils.capitalize(field), field))
                        .append("\n");
                return;
            }

            if (i != fieldNamesWithoutId.size() - 1) {
                sb.append(String.format("                .set%s(%s)", StringUtils.capitalize(field), field))
                        .append("\n");
                return;
            }
            sb.append(String.format("                .set%s(%s);", StringUtils.capitalize(field), field))
                        .append("\n\n");
        });

        sb.append(String.format("        LOGGER.info(\"Updating %s with id {}\", id);", modelDefinition.getName()))
                .append("\n\n")
                .append(String.format("        return this.repository.saveAndFlush(existing);"))
                .append("\n")
                .append("    }");

        return sb.toString();
    }

    /**
     * Generates the deleteById method as a string for the given model.
     * 
     * The generated method takes the ID of the model as a parameter and
     * calls the deleteById method of the repository. It also logs
     * information about the deletion.
     * 
     * @param modelDefinition The model definition for which the deleteById
     *                        method is to be generated.
     * @return A string representation of the deleteById method.
     */
    private String generateDeleteByIdMethod(final ModelDefinition modelDefinition) {

        final StringBuilder sb = new StringBuilder();

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        if (StringUtils.isNotBlank(idField.getDescription())) {

            sb.append("    /**\n")
                    .append(String.format("     * Deletes a {@link %s} by its ID.\n", modelDefinition.getName()))
                    .append("     *\n")
                    .append(String.format("     * @param id %s", idField.getDescription()))
                    .append("\n")
                    .append("     */\n");
        }

        sb.append(String.format("    %s", TRANSACTIONAL_ANNOTATION))
                .append("\n")
                .append(String.format(("    public void deleteById(final %s id) {"), idField.getType()))
                .append("\n")
                .append(String.format("        LOGGER.info(\"Deleting %s with id {}\", id);", modelDefinition.getName()))
                .append("\n\n")
                .append("        this.repository.deleteById(id);")
                .append("\n\n")
                .append(String.format("        LOGGER.info(\"Deleted %s with id {}\", id);", modelDefinition.getName()))
                .append("\n")
                .append("    }");

        return sb.toString();
    }

    /**
     * Generates the getById method as a string for the given model.
     * 
     * The generated method takes the ID of the model as a parameter and
     * calls the findById method of the repository. If the model with the
     * given ID is found, it is returned. Otherwise, a RuntimeException is
     * thrown.
     * 
     * @param modelDefinition The model definition for which the getById
     *                        method is to be generated.
     * @return A string representation of the getById method.
     */
    private String generateGetByIdMethod(final ModelDefinition modelDefinition) {

        final StringBuilder sb = new StringBuilder();

        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        if (StringUtils.isNotBlank(idField.getDescription())) {

            sb.append("    /**\n")
                    .append(String.format("     * Get a {@link %s} by id.\n", modelDefinition.getName()))
                    .append("     *\n")
                    .append(String.format("     * @param id %s", idField.getDescription()))
                    .append("\n")
                    .append(String.format("     * @return Found %s {@link %s}\n", modelDefinition.getName(), modelDefinition.getName()))
                    .append("     */\n");
        }

        sb.append(String.format(("    public %s getById(final %s id) {"), modelDefinition.getName(), idField.getType()))
                .append("\n\n")
                .append("        return this.repository.findById(id)")
                .append("\n")
                .append(String.format("            .orElseThrow(() -> new RuntimeException(\"%s with id not found: \" + id));", modelDefinition.getName()))
                .append("\n")
                .append("    }");

        return sb.toString();
    }

}
