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

package dev.markozivkovic.springcrudgenerator.imports;

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.IMPORT;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import dev.markozivkovic.springcrudgenerator.constants.ImportConstants;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;

/**
 * Computes import statements required for MongoDB model generation.
 */
public final class MongoModelImports {

    private MongoModelImports() {}

    /**
     * Computes import statements for a MongoDB model/helper model.
     *
     * @param modelDefinition model for which imports are resolved
     * @param includeDocumentImport whether @Document import should be included
     * @return formatted import statements
     */
    public static String computeMongoModelImports(final ModelDefinition modelDefinition, final boolean includeDocumentImport) {

        final Set<String> imports = new TreeSet<>();

        if (includeDocumentImport) {
            imports.add(ImportConstants.SpringData.MONGO_DOCUMENT);
        }

        if (FieldUtils.isAnyFieldId(modelDefinition.getFields())) {
            imports.add(ImportConstants.SpringData.MONGO_ID);
        }

        if (!FieldUtils.extractRelationFields(modelDefinition.getFields()).isEmpty()) {
            imports.add(ImportConstants.SpringData.MONGO_DB_REF);
        }

        final boolean auditEnabled = Objects.nonNull(modelDefinition.getAudit()) && modelDefinition.getAudit().isEnabled();
        if (auditEnabled) {
            imports.add(ImportConstants.SpringData.CREATED_DATE);
            imports.add(ImportConstants.SpringData.LAST_MODIFIED_DATE);
        }

        if (FieldUtils.isAnyRelationCollectionList(modelDefinition.getFields())) {
            imports.add(ImportConstants.Java.ARRAY_LIST);
        }

        if (FieldUtils.isAnyRelationCollectionSet(modelDefinition.getFields())) {
            imports.add(ImportConstants.Java.HASH_SET);
        }

        return imports.stream()
                .map(imp -> String.format(IMPORT, imp))
                .collect(Collectors.joining());
    }
}
