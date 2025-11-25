package com.markozivkovic.codegen.utils;

import java.util.Collection;
import java.util.Objects;

public final class ContainerUtils {
    
    private ContainerUtils() {}

    /**
     * Returns true if the collection is null or empty, false otherwise.
     *
     * @param collection the collection to check
     * @return true if the collection is null or empty, false otherwise
     */
    public static boolean isEmpty(final Collection<?> collection) {
        return Objects.isNull(collection) || collection.isEmpty();
    }
}
