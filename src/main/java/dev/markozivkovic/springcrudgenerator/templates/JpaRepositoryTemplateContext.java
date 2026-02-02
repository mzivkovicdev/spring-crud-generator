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

package dev.markozivkovic.springcrudgenerator.templates;

import java.util.HashMap;
import java.util.Map;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.imports.RepositoryImports;
import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;
import dev.markozivkovic.springcrudgenerator.utils.ModelNameUtils;

public class JpaRepositoryTemplateContext {

    private JpaRepositoryTemplateContext() {}
    
    /**
     * Creates a template context for the JPA interface of a model.
     * 
     * @param modelDefinition      the model definition
     * @param openInViewEnabled    whether open in view is enabled
     * @param packagePath          the package path of the project
     * @param packageConfiguration the package configuration of the project
     * @return a template context for the JPA interface
     */
    public static Map<String, Object> computeJpaInterfaceContext(final ModelDefinition modelDefinition,
            final Boolean openInViewEnabled, final String packagePath, final PackageConfiguration packageConfiguration) {
    
        final FieldDefinition idField = FieldUtils.extractIdField(modelDefinition.getFields());

        final Map<String, Object> context = new HashMap<>();
        context.put(TemplateContextConstants.CLASS_NAME, ModelNameUtils.stripSuffix(modelDefinition.getName()) + "Repository");
        context.put(TemplateContextConstants.MODEL_NAME, modelDefinition.getName());
        context.put(TemplateContextConstants.ID_TYPE, idField.getType());
        context.put(TemplateContextConstants.ID_FIELD, idField.getName());
        context.put(
            TemplateContextConstants.ENTITY_GRAPH_NAME,
            ModelNameUtils.computeEntityGraphName(
                modelDefinition.getName(), FieldUtils.extractLazyFetchFieldNames(modelDefinition.getFields())
            )
        );
        context.put(TemplateContextConstants.OPEN_IN_VIEW_ENABLED, openInViewEnabled);
        context.put(TemplateContextConstants.BASE_IMPORTS, RepositoryImports.computeJpaRepostiroyImports(modelDefinition, openInViewEnabled));
        context.put(
            TemplateContextConstants.PROJECT_IMPORTS,
            RepositoryImports.computeProjectImports(packagePath, packageConfiguration, modelDefinition.getName())
        );
        context.put(TemplateContextConstants.HAS_LAZY_FIELDS, FieldUtils.hasLazyFetchField(modelDefinition.getFields()));
        
        return context;
    }
}
