import java.util.Collection;
import java.util.stream.IntStream;

${projectImports}
public class ArgumentVerifier {

    private ArgumentVerifier() {

    }

    /**
     * Check if any of the arguments have null value
     *
     * @param objects Objects to be verified
     * @throws InvalidArgumentException if provided object is null
     */
    public static void verifyNotNull(final Object... objects) {

        IntStream.range(0, objects.length).forEach(i -> {

            final Object obj = objects[i];

            if (obj == null) {

                throw new InvalidArgumentException(
                        String.format(
                                "Provided argument on position [%d] is null.",
                                i
                        )
                );
            }
        });
    }

    /**
     * Method that checks if provided collection is empty or null
     *
     * @param collections Collections to be checked
     * @throws InvalidArgumentException if provided collection is null or empty
     */
    public static void verifyNotEmpty(final Collection<?>... collections) {

        IntStream.range(0, collections.length).forEach(i -> {
            final Collection<?> collection = collections[i];

            if (collection == null || collection.isEmpty()) {
                throw new InvalidArgumentException(
                        String.format(
                                "Provided collection [%d] is empty or null.",
                                i
                        )
                );
            }
        });
    }

    /**
     * Method that checks if provided character sequence is empty or null.
     *
     * @param values values to be checked
     * @throws InvalidArgumentException if provided value is null or empty
     */
    public static void verifyNotEmpty(final CharSequence... values) {

        IntStream.range(0, values.length).forEach(i -> {
            final CharSequence value = values[i];

            if (value == null || value.length() == 0) {
                throw new InvalidArgumentException(
                        String.format(
                                "Provided text argument [%d] is empty or null.",
                                i
                        )
                );
            }
        });
    }

    /**
     * Method that checks if provided character sequence is blank or null.
     *
     * @param values values to be checked
     * @throws InvalidArgumentException if provided value is null or blank
     */
    public static void verifyNotBlank(final CharSequence... values) {

        IntStream.range(0, values.length).forEach(i -> {
            final CharSequence value = values[i];

            if (value == null || value.toString().isBlank()) {
                throw new InvalidArgumentException(
                        String.format(
                                "Provided text argument [%d] is blank or null.",
                                i
                        )
                );
            }
        });
    }
}
