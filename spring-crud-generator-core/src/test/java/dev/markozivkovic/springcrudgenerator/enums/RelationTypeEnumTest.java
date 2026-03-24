package dev.markozivkovic.springcrudgenerator.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RelationTypeEnumTest {

    @Test
    @DisplayName("getDefaultLazyTypes: should contain only OneToMany and ManyToMany")
    void defaultLazyTypes_shouldContainOnlyOneToManyAndManyToMany() {
        final List<String> lazy = RelationTypeEnum.getDefaultLazyTypes();
        assertEquals(List.of("OneToMany", "ManyToMany"), lazy);
    }

    @Test
    @DisplayName("getDefaultEagerTypes: should contain only OneToOne and ManyToOne")
    void defaultEagerTypes_shouldContainOnlyOneToOneAndManyToOne() {
        final List<String> eager = RelationTypeEnum.getDefaultEagerTypes();
        assertEquals(List.of("OneToOne", "ManyToOne"), eager);
    }

    @Test
    @DisplayName("getKey: should return enum key value")
    void getKey_shouldReturnEnumKey() {
        assertEquals("OneToOne", RelationTypeEnum.ONE_TO_ONE.getKey());
        assertEquals("OneToMany", RelationTypeEnum.ONE_TO_MANY.getKey());
        assertEquals("ManyToOne", RelationTypeEnum.MANY_TO_ONE.getKey());
        assertEquals("ManyToMany", RelationTypeEnum.MANY_TO_MANY.getKey());
    }

    @Test
    @DisplayName("fromString: should return matching enum for valid values")
    void fromString_validValues_returnsEnum() {
        assertEquals(RelationTypeEnum.ONE_TO_ONE, RelationTypeEnum.fromString("OneToOne"));
        assertEquals(RelationTypeEnum.ONE_TO_MANY, RelationTypeEnum.fromString("OneToMany"));
        assertEquals(RelationTypeEnum.MANY_TO_ONE, RelationTypeEnum.fromString("ManyToOne"));
        assertEquals(RelationTypeEnum.MANY_TO_MANY, RelationTypeEnum.fromString("ManyToMany"));
    }

    @Test
    @DisplayName("fromString: should throw for invalid value")
    void fromString_invalidValue_throwsException() {
        
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RelationTypeEnum.fromString("OneToManyy")
        );

        assertEquals(
                "No enum constant with value OneToManyy. Possible values are: OneToOne, OneToMany, ManyToOne, ManyToMany",
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("fromString: should throw for null value")
    void fromString_nullValue_throwsException() {
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RelationTypeEnum.fromString(null)
        );

        assertEquals(
                "No enum constant with value null. Possible values are: OneToOne, OneToMany, ManyToOne, ManyToMany",
                ex.getMessage()
        );
    }

    @Test
    @DisplayName("getPossibleValues: should return all relation type values comma separated")
    void getPossibleValues_returnsCommaSeparatedValues() {
        assertEquals(
                "OneToOne, OneToMany, ManyToOne, ManyToMany",
                RelationTypeEnum.getPossibleValues()
        );
    }
}