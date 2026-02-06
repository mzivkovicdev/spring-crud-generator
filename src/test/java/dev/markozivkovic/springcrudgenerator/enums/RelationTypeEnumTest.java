package dev.markozivkovic.springcrudgenerator.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class RelationTypeEnumTest {

    @Test
    void defaultLazyTypes_shouldContainOnlyOneToManyAndManyToMany() {
        final List<String> lazy = RelationTypeEnum.getDefaultLazyTypes();
        assertEquals(List.of("OneToMany", "ManyToMany"), lazy);
    }

    @Test
    void defaultEagerTypes_shouldContainOnlyOneToOneAndManyToOne() {
        final List<String> eager = RelationTypeEnum.getDefaultEagerTypes();
        assertEquals(List.of("OneToOne", "ManyToOne"), eager);
    }
}
