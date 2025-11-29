package com.markozivkovic.codegen.generators;

public interface ProjectArtifactGenerator {
    
    /**
     * Generates the project artifact 
     * 
     * @param outputDir the directory where the generated code will be written
     */
    void generate(final String outputDir);

}
