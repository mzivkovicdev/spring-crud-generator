package com.markozivkovic.codegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;
import com.markozivkovic.codegen.utils.StringUtils;

public class SpringCrudGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCrudGenerator.class);

    public void generate(final ModelDefinition modelDefinition) {
        
        LOGGER.info("Generating model: {}", modelDefinition.getName());
        
        this.generateJpaEntity(modelDefinition);
    }

    /**
     * Generates a Java entity class file for the given model definition.
     *
     * @param model The model definition containing the class name, table name, and field definitions.
     */
    private void generateJpaEntity(final ModelDefinition model) {
        
        final String className = model.getName();
        final String tableName = model.getTableName();
        final List<FieldDefinition> fields = model.getFields();

        final StringBuilder sb = new StringBuilder();

        sb.append("package com.example.generated;\n\n");
        sb.append("import java.util.Objects;\n\n");
        sb.append("import jakarta.persistence.Entity;\n");
        sb.append("import jakarta.persistence.GeneratedValue;\n");
        sb.append("import jakarta.persistence.GenerationType;\n");
        sb.append("import jakarta.persistence.Id;\n");
        sb.append("import jakarta.persistence.Table;\n");
        sb.append("\n");
        sb.append("@Entity\n");
        sb.append("@Table(name = \"" + tableName + "\")\n");
        sb.append("public class " + className + " {\n\n");

        fields.stream().forEach(field -> {
            if (field.isId()) {
                sb.append("    @Id\n");
                sb.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
            }
            sb.append("    private " + field.getType() + " " + field.getName() + ";\n\n");
        });

        sb.append(this.generateGettersAndSetters(model))
            .append(this.generateEqualsMethod(model))
            .append("\n")
            .append(this.generateHashCodeMethod(model))
            .append("\n")
            .append(this.generateToStringMethod(model))
            .append("\n");

        sb.append("}\n");

        final String packagePath = "output/";
        final File outputDir = new File(packagePath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter("output/" + className + ".java")) {
            writer.write(sb.toString());
            System.out.println("Glass generated at: output/" + className + ".java");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates the getters and setters for the given model as a string.
     *
     * The generated code will contain a getter and setter for each field of the model.
     * The getter will return the value of the field, while the setter will set the value
     * of the field and return the instance of the class to allow for method chaining.
     *
     * @param model The model definition for which the getters and setters are to be generated.
     * @return A string representation of the getters and setters.
     */
    private String generateGettersAndSetters(final ModelDefinition model) {

        final StringBuilder sb = new StringBuilder();

        model.getFields().stream().forEach(field -> {
            final String capitalized = StringUtils.capitalize(field.getName());

            // Getter
            sb.append(String.format("    public %s get%s() {\n", field.getType(), capitalized));
            sb.append(String.format("        return this.%s;\n", field.getName()));
            sb.append("    }\n\n");

            // Setter
            sb.append(String.format("    public %s set%s(final %s %s) {\n", model.getName(), capitalized, field.getType(), field.getName()));
            sb.append("        this." + field.getName() + " = " + field.getName() + ";\n");
            sb.append("        return this;\n");
            sb.append("    }\n\n");
        });

        return sb.toString();
    }

    /**
     * Generates the toString method as a string for the given model.
     * The generated method returns a string representation of the model
     * by iterating over its fields and appending their names and values,
     * formatted as key-value pairs within curly braces. For fields of 
     * type boolean or Boolean, the method uses "is" as the getter prefix, 
     * while for other fields it uses "get".
     * 
     * @param model The model definition for which the toString method is to be generated.
     * @return A string representation of the toString method.
     */
    private String generateToStringMethod(final ModelDefinition model) {
        
        final StringBuilder sb = new StringBuilder();

        sb.append("    @Override\n");
        sb.append("    public String toString() {\n");
        sb.append("        return \"{\" +\n");

        final List<FieldDefinition> fields = model.getFields();
        
        IntStream.range(0, fields.size()).forEach(i -> {
            final FieldDefinition field = fields.get(i);
            final String fieldName = field.getName();
            final String getter = (field.getType().equals("boolean") || field.getType().equals("Boolean") ? "is" : "get")
                    + StringUtils.capitalize(fieldName);
    
            if (i == 0) {
                sb.append("            \" ").append(fieldName).append("='\" + ").append(getter).append("() + \"'\" +\n");
            } else {
                sb.append("            \", ").append(fieldName).append("='\" + ").append(getter).append("() + \"'\" +\n");
            }
        });

        sb.append("            \"}\";\n");
        sb.append("    }\n");

        return sb.toString();
    }

    /**
     * Generates the equals method as a string for the given model.
     * The generated method compares the fields of the model for equality.
     * 
     * @param model The model definition for which the equals method is to be generated.
     * @return A string representation of the equals method.
     */
    private String generateEqualsMethod(final ModelDefinition model) {
        
        final String className = model.getName();
        final StringBuilder sb = new StringBuilder();
        
        sb.append("    @Override\n");
        sb.append("    public boolean equals(final Object o) {\n");
        sb.append("        if (o == this)\n");
        sb.append("            return true;\n");
        sb.append("        if (!(o instanceof ").append(className).append(")) {\n");
        sb.append("            return false;\n");
        sb.append("        }\n");
        sb.append("        final ").append(className).append(" other = (").append(className).append(") o;\n");
        sb.append("        return ");

        final List<String> fields = model.getFields().stream()
            .map(FieldDefinition::getName)
            .map(field -> "Objects.equals(" + field + ", other." + field + ")")
            .collect(Collectors.toList());

        sb.append(String.join("\n                && ", fields)).append(";\n");
        sb.append("    }\n");

        return sb.toString();
    }

    /**
     * Generates the hashCode method as a string for the given model.
     * The generated method uses the Objects.hash method to compute the hash code
     * of the model.
     * 
     * @param model The model definition for which the hashCode method is to be generated.
     * @return A string representation of the hashCode method.
     */
    private String generateHashCodeMethod(final ModelDefinition model) {
        final StringBuilder sb = new StringBuilder();

        sb.append("    @Override\n");
        sb.append("    public int hashCode() {\n");
        sb.append("        return Objects.hash(");

        final List<String> fields = model.getFields().stream()
            .map(FieldDefinition::getName)
            .collect(Collectors.toList());

        sb.append(String.join(", ", fields));
        sb.append(");\n");
        sb.append("    }\n");

        return sb.toString();
    }
    
}
