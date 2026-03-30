package dev.markozivkovic.springcrudgenerator.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContainerUtilsTest {

    @Test
    @DisplayName("isEmpty(Collection) should return true when collection is null")
    void isEmptyCollection_shouldReturnTrue_whenNull() {
        
        final Collection<?> collection = null;

        assertTrue(ContainerUtils.isEmpty(collection));
    }

    @Test
    @DisplayName("isEmpty(Collection) should return true when collection is empty")
    void isEmptyCollection_shouldReturnTrue_whenEmpty() {
        
        final Collection<?> collection = List.of();

        assertTrue(ContainerUtils.isEmpty(collection));
    }

    @Test
    @DisplayName("isEmpty(Collection) should return false when collection has elements")
    void isEmptyCollection_shouldReturnFalse_whenNotEmpty() {
        
        final Collection<?> collection = List.of("value");

        assertFalse(ContainerUtils.isEmpty(collection));
    }

    @Test
    @DisplayName("isEmpty(Map) should return true when map is null")
    void isEmptyMap_shouldReturnTrue_whenNull() {
        
        final Map<?, ?> map = null;

        assertTrue(ContainerUtils.isEmpty(map));
    }

    @Test
    @DisplayName("isEmpty(Map) should return true when map is empty")
    void isEmptyMap_shouldReturnTrue_whenEmpty() {
        
        final Map<?, ?> map = Map.of();

        assertTrue(ContainerUtils.isEmpty(map));
    }

    @Test
    @DisplayName("isEmpty(Map) should return false when map has elements")
    void isEmptyMap_shouldReturnFalse_whenNotEmpty() {
        
        final Map<?, ?> map = Map.of("key", "value");

        assertFalse(ContainerUtils.isEmpty(map));
    }
}
