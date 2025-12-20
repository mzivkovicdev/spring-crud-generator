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

package com.markozivkovic.codegen.context;

import java.util.HashSet;
import java.util.Set;

public class GeneratorContext {
    
    private static final Set<String> GENERATED_PARTS = new HashSet<>();

    private GeneratorContext() {

    }

    /**
     * Returns true if the given part has already been generated.
     * 
     * @param part the part to check
     * @return true if the part has already been generated
     */
    public static boolean isGenerated(final String part) {
        return GENERATED_PARTS.contains(part);
    }

    /**
     * Marks the given part as generated.
     * 
     * @param part the part to mark
     * @return true if the part was not already generated
     */
    public static boolean markGenerated(final String part) {
        return GENERATED_PARTS.add(part);
    }

}
