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

package dev.markozivkovic.codegen.constants;

public class AnnotationConstants {
    
    private AnnotationConstants(){

    }

    public static final String ID_ANNOTATION = "@Id";
    public static final String GENERATED_VALUE_ANNOTATION = "@GeneratedValue(strategy = GenerationType.IDENTITY)";
    public static final String ENTITY_ANNOTATION = "@Entity";
    public static final String TABLE_ANNOTATION = "@Table(name = \"%s\")";
    public static final String ENTITY_LISTENERS_ANNOTATION = "@EntityListeners({%s})";
    public static final String AUDITING_ENTITY_LISTENER_CLASS = "AuditingEntityListener.class";
    public static final String TRANSACTIONAL_ANNOTATION = "@Transactional";
}
