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

package com.markozivkovic.codegen.templates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.markozivkovic.codegen.constants.TemplateContextConstants;

public class EnumTemplateContext {

    private EnumTemplateContext() {}

    /**
     * Creates a template context for the enum class of a model.
     * 
     * @param enumName the name of the enum
     * @param enumValues the values of the enum
     * @return a template context for the enum class
     */
    public static Map<String, Object> createEnumContext(final String enumName, final List<String> enumValues) {

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.ENUM_NAME, enumName);
        context.put(TemplateContextConstants.VALUES, enumValues);
        return context;
    }
    
}
