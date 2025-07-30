package com.markozivkovic.codegen.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWriterUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileWriterUtils.class);

    private FileWriterUtils() {
        
    }

    /**
     * Writes the given content to a file in the specified directory. The file
     * name is given as the last part of the subDir parameter.
     * 
     * @param outputDir   the root directory where the file should be written
     * @param subDir      the subdirectory within the outputDir where the file should
     *                    be written
     * @param fileName    the name of the file to write, without the .java extension
     * @param content     the content of the file to write
     */
    public static void writeToFile(final String outputDir, final String subDir, final String fileName,
            final String content) {
        
        final File directory = new File(outputDir + File.separator + subDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        final File file = new File(directory, fileName + ".java");

        try (final FileWriter writer = new FileWriter(file)) {
            writer.write(content);
            LOGGER.info("Generated class: {}", fileName);
        } catch (final IOException e) {
            LOGGER.error("Failed to generate class {}: {}", fileName, e.getMessage());
            throw new RuntimeException(
                String.format("Failed to generate class file: %s", fileName), e
            );
        }
    }
    
}
