package com.markozivkovic.codegen.utils;

import java.util.Objects;

import com.markozivkovic.codegen.models.CrudConfiguration.DockerConfiguration;

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
