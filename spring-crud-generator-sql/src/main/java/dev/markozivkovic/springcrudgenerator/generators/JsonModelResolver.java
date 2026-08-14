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

package dev.markozivkovic.springcrudgenerator.generators;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.markozivkovic.springcrudgenerator.enums.BasicTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;

final class JsonModelResolver {

    private JsonModelResolver() {
    }

    static List<ModelDefinition> resolveReferencedModels(
            final List<ModelDefinition> modelsToInspect,
            final List<ModelDefinition> activeModels) {

        final Map<String, ModelDefinition> activeModelsByName = activeModels.stream()
                .collect(Collectors.toMap(ModelDefinition::getName, Function.identity()));

        return modelsToInspect.stream()
                .flatMap(model -> model.getFields().stream())
                .filter(FieldUtils::isJsonField)
                .map(FieldUtils::extractJsonInnerElementType)
                .filter(jsonType -> !BasicTypeEnum.isBasicType(jsonType))
                .distinct()
                .map(jsonType -> requireModel(jsonType, activeModelsByName))
                .toList();
    }

    private static ModelDefinition requireModel(
            final String modelName,
            final Map<String, ModelDefinition> activeModelsByName) {

        final ModelDefinition model = activeModelsByName.get(modelName);
        if (model == null) {
            throw new IllegalArgumentException(
                    "JSON model is not present in the active model graph: " + modelName);
        }
        return model;
    }
}
