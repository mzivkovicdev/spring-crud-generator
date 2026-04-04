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

package dev.markozivkovic.springcrudgenerator.validators;

import java.util.Objects;

import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.ApplicationDockerConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DbDockerConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DockerConfiguration;

public class DockerConfigurationValidator {
    
    private DockerConfigurationValidator() {}

    /**
     * Validates the docker configuration.
     * 
     * If the docker configuration is null, it simply returns.
     * If the docker configuration has dockerfile set to false, but app or db are set, it throws an
     * {@link IllegalArgumentException}.
     * It also validates the application and database docker configurations.
     * 
     * @param docker the docker configuration to be validated
     */
    public static void validate(final DockerConfiguration docker) {

        if (Objects.isNull(docker)) return;

        if (Boolean.FALSE.equals(docker.getDockerfile()) &&
                (Objects.nonNull(docker.getApp()) || Objects.nonNull(docker.getDb()))) {
            throw new IllegalArgumentException(
                """
                        Invalid docker configuration: docker.dockerfile set to be false, but app or db are set.
                        Please set docker.dockerfile to true or just remove it and keep app and db.
                        """
            );
        }

        validateApp(docker.getApp());
        validateDb(docker.getDb());
    }

    /**
     * Validates the application docker configuration.
     * 
     * If the application docker configuration is not null, it checks if the port is valid.
     * If the port is not null and is not between 1 and 65535, it throws an
     * {@link IllegalArgumentException}.
     * 
     * @param app the application docker configuration
     */
    private static void validateApp(final ApplicationDockerConfiguration app) {
        
        if (Objects.isNull(app)) return;
        
        if (Objects.nonNull(app.getPort()) && (app.getPort() < 1 || app.getPort() > 65535)) {
            throw new IllegalArgumentException(
                """
                        Invalid docker configuration: app.port must be between 1 and 65535.
                        """
            );
        }
    }

    /**
     * Validates the database docker configuration.
     * 
     * If the database docker configuration is not null, it checks if the port is valid.
     * If the port is not null and is not between 1 and 65535, it throws an
     * {@link IllegalArgumentException}.
     * 
     * @param db the database docker configuration
     */
    private static void validateDb(final DbDockerConfiguration db) {
        
        if (Objects.isNull(db)) return;
        
        if (Objects.nonNull(db.getPort()) && (db.getPort() < 1 || db.getPort() > 65535)) {
            throw new IllegalArgumentException(
                """
                        Invalid docker configuration: db.port must be between 1 and 65535.
                        """
            );
        }
    }

}
