package com.markozivkovic.codegen.imports.common;

import java.util.Set;

public class ImportCommon {
    
    private ImportCommon() {}

    /**
     * Adds the given value to the given set if the condition is true.
     *
     * @param condition The condition to check.
     * @param set       The set to add to.
     * @param value     The value to add.
     */
    public static void addIf(final boolean condition, final Set<String> set, final String value) {
        if (condition) {
            set.add(value);
        }
    }

}
