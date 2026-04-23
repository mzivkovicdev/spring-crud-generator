import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

${projectImports}
class ArgumentVerifierTest {

    @Test
    void verifyNotNull_shouldThrowWhenNullIsProvided() {
        final InvalidArgumentException ex = assertThrows(
                InvalidArgumentException.class,
                () -> ArgumentVerifier.verifyNotNull("a", null)
        );

        assertEquals("Provided argument on position [1] is null.", ex.getMessage());
    }

    @Test
    void verifyNotEmptyCollection_shouldThrowWhenCollectionIsEmpty() {
        final InvalidArgumentException ex = assertThrows(
                InvalidArgumentException.class,
                () -> ArgumentVerifier.verifyNotEmpty(List.of())
        );

        assertEquals("Provided collection [0] is empty or null.", ex.getMessage());
    }

    @Test
    void verifyNotEmptyText_shouldThrowWhenTextIsEmpty() {
        final InvalidArgumentException ex = assertThrows(
                InvalidArgumentException.class,
                () -> ArgumentVerifier.verifyNotEmpty("")
        );

        assertEquals("Provided text argument [0] is empty or null.", ex.getMessage());
    }

    @Test
    void verifyNotBlank_shouldThrowWhenTextIsBlank() {
        final InvalidArgumentException ex = assertThrows(
                InvalidArgumentException.class,
                () -> ArgumentVerifier.verifyNotBlank("   ")
        );

        assertEquals("Provided text argument [0] is blank or null.", ex.getMessage());
    }

    @Test
    void verifyMethods_shouldNotThrowWhenValuesAreValid() {
        assertDoesNotThrow(() -> {
            ArgumentVerifier.verifyNotNull("name", 1L);
            ArgumentVerifier.verifyNotEmpty(List.of("v"));
            ArgumentVerifier.verifyNotEmpty("ok");
            ArgumentVerifier.verifyNotBlank("ok");
        });
    }
}
