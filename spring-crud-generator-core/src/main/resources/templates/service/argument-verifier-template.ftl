import java.util.Collection;
import java.util.stream.IntStream;

import org.springframework.util.CollectionUtils;

public class ArgumentVerifier {

    private ArgumentVerifier() {

    }

    /**
     * Check if any of the arguments have null value
     * 
     * @param objects Objects to be verified
     * @throws EtArgumentException if provided object is null
     */
    public static void verifyNotNull(final Object... objects) {

        IntStream.range(0, objects.length).forEach(i -> {

            final Object obj = objects[i];

            if (obj == null) {

                throw new EtArgumentException(
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
     * @throws EtArgumentException if provided collection is null or empty
     */
    public static void verifyNotEmpty(final Collection<?>... collections) {

        IntStream.range(0, collections.length).forEach(i -> {
            final Collection<?> collection = collections[i];

            if (CollectionUtils.isEmpty(collection)) {
                throw new EtArgumentException(
                        String.format(
                                "Provided collection [%d] is empty or null.",
                                i
                        )
                );
            }
        });
    }
}
