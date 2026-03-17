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

package dev.markozivkovic.springcrudgenerator.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.SortDefinition;
import dev.markozivkovic.springcrudgenerator.models.SortDirection;

public class SortUtils {

    private SortUtils() {}

    /**
     * Checks whether sorting is enabled for the given model.
     *
     * @param modelDefinition model definition to inspect
     * @return true when model has sort enabled, false otherwise
     */
    public static boolean isSortEnabled(final ModelDefinition modelDefinition) {

        return Objects.nonNull(modelDefinition)
                && Objects.nonNull(modelDefinition.getSort());
    }

    /**
     * Resolves allowed sort fields for the given model.
     *
     * @param modelDefinition model definition to inspect
     * @return immutable list of allowed fields, or empty list when sorting is disabled
     */
    public static List<String> resolveAllowedFields(final ModelDefinition modelDefinition) {

        if (!isSortEnabled(modelDefinition) || Objects.isNull(modelDefinition.getSort().getAllowedFields())) {
            return List.of();
        }

        return Collections.unmodifiableList(new ArrayList<>(modelDefinition.getSort().getAllowedFields()));
    }

    /**
     * Resolves default sort field for the given model.
     *
     * @param modelDefinition model definition to inspect
     * @return default field when sorting is enabled, otherwise null
     */
    public static String resolveDefaultField(final ModelDefinition modelDefinition) {

        if (!isSortEnabled(modelDefinition)) {
            return null;
        }

        return modelDefinition.getSort().getDefaultField();
    }

    /**
     * Resolves default sort direction for the given model.
     *
     * @param modelDefinition model definition to inspect
     * @return direction name, defaults to ASC when not configured
     */
    public static String resolveDefaultDirection(final ModelDefinition modelDefinition) {

        if (!isSortEnabled(modelDefinition)) {
            return SortDirection.ASC.name();
        }

        final SortDefinition sort = modelDefinition.getSort();
        if (Objects.isNull(sort.getDefaultDirection())) {
            return SortDirection.ASC.name();
        }

        return sort.getDefaultDirection().name();
    }

    /**
     * Resolves allowMultiple flag for sort configuration.
     *
     * @param modelDefinition model definition to inspect
     * @return true if allowMultiple=true and sorting is enabled
     */
    public static boolean isAllowMultiple(final ModelDefinition modelDefinition) {

        return isSortEnabled(modelDefinition)
                && Boolean.TRUE.equals(modelDefinition.getSort().getAllowMultiple());
    }

    /**
     * Contributes sort-related keys to the provided template context map.
     *
     * @param modelDefinition model definition to inspect
     * @param context template context map to enrich
     */
    public static void contributeSortContext(final ModelDefinition modelDefinition, final Map<String, Object> context) {

        final List<String> allowedFields = resolveAllowedFields(modelDefinition);
        context.put(TemplateContextConstants.SORT_ENABLED, isSortEnabled(modelDefinition));
        context.put(TemplateContextConstants.SORT_ALLOWED_FIELDS, allowedFields);
        context.put(TemplateContextConstants.SORT_ALLOWED_FIELDS_CSV, String.join(", ", allowedFields));
        context.put(TemplateContextConstants.SORT_DEFAULT_FIELD, resolveDefaultField(modelDefinition));
        context.put(TemplateContextConstants.SORT_DEFAULT_DIRECTION, resolveDefaultDirection(modelDefinition));
        context.put(TemplateContextConstants.SORT_ALLOW_MULTIPLE, isAllowMultiple(modelDefinition));
    }
}
