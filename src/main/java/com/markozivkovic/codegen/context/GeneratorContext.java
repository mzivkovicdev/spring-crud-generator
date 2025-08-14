package com.markozivkovic.codegen.context;

import java.util.HashSet;
import java.util.Set;

public class GeneratorContext {
    
    private static final Set<String> GENERATED_PARTS = new HashSet<>();

    private GeneratorContext() {

    }

    public static boolean isGenerated(final String part) {
        return GENERATED_PARTS.contains(part);
    }

    public static boolean markGenerated(final String part) {
        return GENERATED_PARTS.add(part);
    }

}
