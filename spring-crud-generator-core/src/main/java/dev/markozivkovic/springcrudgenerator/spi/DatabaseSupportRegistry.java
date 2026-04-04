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

import java.util.ServiceLoader;

import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DatabaseType;

/**
 * Registry that discovers {@link DatabaseSupport} implementations via Java {@link ServiceLoader}.
 *
 * <p>Each database module registers its implementation in
 * {@code META-INF/services/dev.markozivkovic.springcrudgenerator.spi.DatabaseSupport}.
 */
public final class DatabaseSupportRegistry {

    private DatabaseSupportRegistry() {}

    /**
     * Resolves the {@link DatabaseSupport} implementation for the given database type.
     *
     * @param dbType the database type to look up
     * @return the matching {@link DatabaseSupport} implementation
     * @throws IllegalStateException if no implementation is found for the given database type
     */
    public static DatabaseSupport resolve(final DatabaseType dbType) {

        final ServiceLoader<DatabaseSupport> loader = ServiceLoader.load(DatabaseSupport.class);

        for (final DatabaseSupport support : loader) {
            if (support.supports(dbType)) {
                return support;
            }
        }

        throw new IllegalStateException(String.format(
            "No DatabaseSupport implementation found for database type: %s. "
                + "Ensure the appropriate database module is on the classpath.",
            dbType
        ));
    }
}
