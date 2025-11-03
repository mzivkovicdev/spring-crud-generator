package com.markozivkovic.codegen.utils;

import java.io.File;
import java.util.Arrays;

public class FileUtils {
    
    private FileUtils() {

    }

    /**
     * Joins the given string parts into a single string, separated by the file system's path separator.
     * Each part is trimmed and any leading or trailing path separators are removed.
     * If any part is null or empty, it is ignored.
     * The resulting string will not have any trailing path separators.
     * 
     * @param parts the string parts to join
     * @return the joined string
     */
    public static String join(final String... parts) {

        final StringBuilder sb = new StringBuilder();
        
        Arrays.asList(parts).forEach(part -> {
            if (StringUtils.isNotBlank(part)) {
                final String parsed = part.trim();
                if (sb.length() > 0) {
                    sb.append(File.separator);
                }
                sb.append(parsed);
            }
        });

        return sb.toString();
    }

}
