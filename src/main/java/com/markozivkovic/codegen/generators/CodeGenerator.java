package com.markozivkovic.codegen.generators;

import com.markozivkovic.codegen.model.ModelDefinition;

public interface CodeGenerator {
    
    /**
     * Generates code based on the provided model definition and writes it to the specified output directory.
     *
     * @param modelDefinition the model definition containing the details for code generation
     * @param outputDir       the directory where the generated code will be written
     */
    void generate(final ModelDefinition modelDefinition, final String outputDir);

}
