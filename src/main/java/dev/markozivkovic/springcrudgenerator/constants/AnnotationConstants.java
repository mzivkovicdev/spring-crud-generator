/*
 * Copyright 2025-present Marko Zivkovic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.markozivkovic.springcrudgenerator.constants;

/**
 * Constants for commonly used annotations in the code generation process.
 */
public class AnnotationConstants {
    
    private AnnotationConstants(){

    }

    public static final String ID_ANNOTATION = "@Id";
    public static final String TRANSACTIONAL_ANNOTATION = "@Transactional";

    public static final String NOT_NULL_ANNOTATION = "@NotNull";
    public static final String NOT_EMPTY_ANNOTATION = "@NotEmpty";
    public static final String NOT_BLANK_ANNOTATION = "@NotBlank";
    public static final String SIZE_MIN_ANNOTATION = "@Size(min = %d)";
    public static final String SIZE_MAX_ANNOTATION = "@Size(max = %d)";
    public static final String SIZE_MIN_MAX_ANNOTATION = "@Size(min = %d, max = %d)";
    public static final String EMAIL_ANNOTATION = "@Email";
    public static final String MIN_ANNOTATION = "@Min(%d)";
    public static final String MAX_ANNOTATION = "@Max(%d)";
    public static final String DECIMAL_MIN_ANNOTATION = "@DecimalMin(\"%s\")";
    public static final String DECIMAL_MAX_ANNOTATION = "@DecimalMax(\"%s\")";
    public static final String PATTERN_ANNOTATION = "@Pattern(regexp = \"%s\")";
}
