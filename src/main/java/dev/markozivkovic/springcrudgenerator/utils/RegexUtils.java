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
    
}
