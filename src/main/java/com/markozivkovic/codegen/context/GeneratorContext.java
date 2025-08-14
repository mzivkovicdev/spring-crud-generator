package com.markozivkovic.codegen.context;

import java.util.HashSet;
import java.util.Set;

public class GeneratorContext {
    
    private static final Set<String> GENERATED_PARTS = new HashSet<>();

    private GeneratorContext() {

    }

    /**
     * Returns true if the given part has already been generated.
     * 
     * @param part the part to check
     * @return true if the part has already been generated
     */
    public static boolean isGenerated(final String part) {
        return GENERATED_PARTS.contains(part);
    }

    /**
     * Marks the given part as generated.
     * 
     * @param part the part to mark
     * @return true if the part was not already generated
     */
    public static boolean markGenerated(final String part) {
        return GENERATED_PARTS.add(part);
    }

}
