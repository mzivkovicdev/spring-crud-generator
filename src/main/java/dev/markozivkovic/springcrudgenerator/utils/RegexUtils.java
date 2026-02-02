package dev.markozivkovic.springcrudgenerator.utils;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class RegexUtils {

    private RegexUtils() {}

    /**
     * Returns true if the given regex is valid, false otherwise.
     * Note that empty strings are considered invalid regexes.
     * 
     * @param regex the regex to validate
     * @return true if the regex is valid, false otherwise
     */
    public static boolean isValidRegex(final String regex) {
        
        if (StringUtils.isBlank(regex)) return false;
        
        try {
            Pattern.compile(regex);
            return true;
        } catch (final PatternSyntaxException ex) {
            return false;
        }
    }

    /**
     * Returns a normalized version of the given regex pattern.
     * The pattern is normalized by:
     * - replacing all backslashes with double backslashes
     * - replacing all double quotes with escaped double quotes
     * - replacing all percent signs with double percent signs
     * 
     * @param pattern the regex pattern to normalize
     * @return the normalized regex pattern
     */
    public static String normalizeRegexPattern(final String pattern) {

        if (StringUtils.isBlank(pattern)) {
            return "";
        }

        return pattern.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("%", "%%'");
    }
    
}
