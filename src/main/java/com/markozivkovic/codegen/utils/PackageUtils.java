package com.markozivkovic.codegen.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class PackageUtils {

    private static final String SOURCE_JAVA = "src/main/java/";
    
    private PackageUtils() {
        
    }

    /**
     * Given an output directory, returns the package path of the generated Java source files.
     * The output directory must be an absolute path and must contain the source Java directory.
     * 
     * @param outputDir the output directory
     * @return the package path of the generated Java source files
     * @throws IllegalArgumentException if the output directory is null or empty, is not an absolute path, or does 
     *                                  not contain the source Java directory
     */
    public static String getPackagePathFromOutputDir(final String outputDir) {
        
        if (!StringUtils.isNotBlank(outputDir)) {
            throw new IllegalArgumentException("Output directory cannot be null or empty");
        }
        
        final Path absoluteOutputDir = Paths.get(outputDir);
        
        if (!absoluteOutputDir.isAbsolute()) {
            throw new IllegalArgumentException("Output directory must be an absolute path");
        }

        final String outputPathStr = absoluteOutputDir.toString();

        if (!outputPathStr.contains(SOURCE_JAVA)) {
            throw new IllegalArgumentException(
                String.format(
                    "Output directory '%s' does not contain the source Java directory '%s'",
                    outputPathStr, SOURCE_JAVA
                )
            );
        }

        final String relativePackagePath = outputPathStr.substring(outputPathStr.indexOf(SOURCE_JAVA) + SOURCE_JAVA.length());
        
        return relativePackagePath.replace(File.separator, ".");
    }

    /**
     * Joins the given string parts into a single string, separated by dots.
     * Each part is trimmed and any leading or trailing dots are removed.
     * If any part is null or empty, it is ignored.
     * The resulting string will not have any trailing dots.
     * 
     * @param parts the string parts to join
     * @return the joined string
     */
    public static String join(final String ...parts) {

        final StringBuilder sb = new StringBuilder();
        
        Arrays.asList(parts).forEach(part -> {
            if (StringUtils.isNotBlank(part)) {
                final String parsed = part.trim();
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(parsed);
            }
        });

        return sb.toString();
    }

}
