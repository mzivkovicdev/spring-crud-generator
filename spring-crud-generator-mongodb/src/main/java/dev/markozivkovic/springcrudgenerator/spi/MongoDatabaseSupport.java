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

package dev.markozivkovic.springcrudgenerator.spi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.markozivkovic.springcrudgenerator.generators.CodeGenerator;
import dev.markozivkovic.springcrudgenerator.generators.MongockMigrationGenerator;
import dev.markozivkovic.springcrudgenerator.generators.MongoEntityGenerator;
import dev.markozivkovic.springcrudgenerator.generators.MongoRepositoryGenerator;
import dev.markozivkovic.springcrudgenerator.licensing.LicenseFeature;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;

/**
 * MongoDB {@link DatabaseSupport} implementation.
 * Registered via META-INF/services for automatic discovery by {@link DatabaseSupportRegistry}.
 *
 * <p>MongoDB support requires a valid license key.
 */
public class MongoDatabaseSupport implements DatabaseSupport {

    @Override
    public boolean supports(final CrudConfiguration.DatabaseType database) {
        return database == CrudConfiguration.DatabaseType.MONGODB;
    }

    @Override
    public Map<String, CodeGenerator> createCodeGenerators(
            final CrudConfiguration config,
            final List<ModelDefinition> entities,
            final ProjectMetadata metadata,
            final PackageConfiguration packages) {

        final Map<String, CodeGenerator> generators = new LinkedHashMap<>();
        generators.put("mongo-model", new MongoEntityGenerator(config, entities, packages));
        generators.put("mongo-repository", new MongoRepositoryGenerator(packages));
        generators.put("mongock-migration-script", new MongockMigrationGenerator(config, metadata, entities));
        return generators;
    }

    @Override
    public boolean requiresLicense() {
        return true;
    }

    @Override
    public LicenseFeature requiredFeature() {
        return LicenseFeature.MONGODB;
    }
}
