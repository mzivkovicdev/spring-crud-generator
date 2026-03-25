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

import java.util.List;
import java.util.Map;

import dev.markozivkovic.springcrudgenerator.generators.CodeGenerator;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.models.ProjectMetadata;

/**
 * SPI (Service Provider Interface) for database-specific code generation support.
 *
 * <p>Each database module (sql, mongodb, etc.) implements this interface and registers
 * itself via Java {@link java.util.ServiceLoader} (META-INF/services).
 *
 * <p>The core orchestrator ({@link dev.markozivkovic.springcrudgenerator.generators.SpringCrudGenerator})
 * discovers implementations at runtime and delegates to the appropriate one based on the
 * configured database type.
 */
public interface DatabaseSupport {

    /**
     * Returns true if this implementation supports the given database type.
     *
     * @param database the database type from the CRUD spec configuration
     * @return true if this implementation handles the given database type
     */
    boolean supports(CrudConfiguration.DatabaseType database);

    /**
     * Creates the named map of database-specific code generators to register with the orchestrator.
     *
     * @param config   the CRUD configuration
     * @param entities the list of entity definitions
     * @param metadata the project metadata
     * @param packages the package configuration
     * @return a named map of generators to register (name → generator)
     */
    Map<String, CodeGenerator> createCodeGenerators(
        CrudConfiguration config,
        List<ModelDefinition> entities,
        ProjectMetadata metadata,
        PackageConfiguration packages
    );

}
