package com.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileWriterUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("writeToFile(outputDir, subDir, fileName, content) creates directories and .java file when fileName has no extension")
    void writeToFile_withSubDir_shouldCreateJavaFile_whenFileNameWithoutExtension() throws IOException {

        final String outputDir = tempDir.toString();
        final String subDir = "com.example.app.service";
        final String fileName = "MyService";
        final String content = "public class MyService {}";

        FileWriterUtils.writeToFile(outputDir, subDir, fileName, content);

        final Path expectedDir = tempDir
                .resolve("com")
                .resolve("example")
                .resolve("app")
                .resolve("service");
        assertTrue(Files.exists(expectedDir), "Expected normalized directory structure to be created");

        final Path expectedFile = expectedDir.resolve("MyService.java");
        assertTrue(Files.exists(expectedFile), "Expected .java file to be created");

        final String writtenContent = Files.readString(expectedFile);
        assertEquals(content, writtenContent);
    }

    @Test
    @DisplayName("writeToFile(outputDir, subDir, fileName, content) respects existing extension in fileName")
    void writeToFile_withSubDir_shouldNotAppendJavaExtension_whenFileNameContainsDot() throws IOException {
        
        final String outputDir = tempDir.toString();
        final String subDir = "com/example/util";
        final String fileName = "Custom.txt";
        final String content = "some text content";

        FileWriterUtils.writeToFile(outputDir, subDir, fileName, content);

        final Path expectedDir = tempDir
                .resolve("com")
                .resolve("example")
                .resolve("util");
        assertTrue(Files.exists(expectedDir));

        Path expectedFile = expectedDir.resolve("Custom.txt");
        assertTrue(Files.exists(expectedFile), "Expected file to be created with original extension");

        String writtenContent = Files.readString(expectedFile);
        assertEquals(content, writtenContent);
    }

    @Test
    @DisplayName("writeToFile(outputPath, fileName, content) creates directory if missing and writes file with exact name")
    void writeToFile_simpleOverload_shouldCreateDirectoryAndFile() throws IOException {
        
        final Path outputPath = tempDir.resolve("generated");
        final String fileName = "TestClass.java";
        final String content = "public class TestClass {}";

        FileWriterUtils.writeToFile(outputPath.toString(), fileName, content);

        assertTrue(Files.exists(outputPath), "Expected output directory to be created");

        final Path expectedFile = outputPath.resolve(fileName);
        assertTrue(Files.exists(expectedFile), "Expected file to be created");

        final String writtenContent = Files.readString(expectedFile);
        assertEquals(content, writtenContent);
    }

    @Test
    @DisplayName("writeToFile(outputDir, subDir, fileName, content) normalizes backslashes and slashes in subDir")
    void writeToFile_withSubDir_shouldNormalizeAllSeparators() throws IOException {
        
        final String outputDir = tempDir.toString();
        final String subDir = "com\\example/service.util";
        final String fileName = "MixedPaths";
        final String content = "class MixedPaths {}";

        FileWriterUtils.writeToFile(outputDir, subDir, fileName, content);

        final Path expectedDir = tempDir
                .resolve("com")
                .resolve("example")
                .resolve("service")
                .resolve("util");

        assertTrue(Files.exists(expectedDir), "Expected normalized directory from mixed separators");

        Path expectedFile = expectedDir.resolve("MixedPaths.java");
        assertTrue(Files.exists(expectedFile));
        assertEquals(content, Files.readString(expectedFile));
    }
}
