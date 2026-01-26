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

import java.util.Objects;

import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DockerConfiguration;

public class DockerUtils {
    
    private DockerUtils() {}

    /**
     * Returns true if the docker configuration specifies that a Dockerfile should be generated, or if the
     * application or database docker configurations are set.
     *
     * @param docker the docker configuration to be checked
     * @return true if the Dockerfile should be generated, false otherwise
     */
    public static boolean isDockerfileEnabled(final DockerConfiguration docker) {

        if (Objects.isNull(docker)) return false;

        if (Boolean.TRUE.equals(docker.getDockerfile())) return true;

        if (Boolean.FALSE.equals(docker.getDockerfile())) return false;

        if (Objects.nonNull(docker.getApp()) || Objects.nonNull(docker.getDb())) return true;

        return false;
    }

}
