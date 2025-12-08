package com.markozivkovic.codegen.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.markozivkovic.codegen.models.CrudConfiguration.ApplicationDockerConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.DbDockerConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.DockerConfiguration;

class DockerUtilsTest {

    private DockerConfiguration config(final Boolean dockerfile, final ApplicationDockerConfiguration app,
            final DbDockerConfiguration db) {
                
        final DockerConfiguration cfg = new DockerConfiguration();
        cfg.setDockerfile(dockerfile)
                .setApp(app)
                .setDb(db);
        return cfg;
    }

    @Test
    @DisplayName("Return false when docker configuration is null")
    void isDockerfileEnabled_shouldReturnFalse_whenNull() {

        assertFalse(DockerUtils.isDockerfileEnabled(null));
    }

    @Test
    @DisplayName("Return true when dockerfile is explicitly TRUE")
    void isDockerfileEnabled_shouldReturnTrue_whenDockerfileTrue() {

        final DockerConfiguration docker = config(true, null, null);

        assertTrue(DockerUtils.isDockerfileEnabled(docker));
    }

    @Test
    @DisplayName("Return false when dockerfile is explicitly FALSE")
    void isDockerfileEnabled_shouldReturnFalse_whenDockerfileFalse() {
        
        final DockerConfiguration docker = config(false, null, null);

        assertFalse(DockerUtils.isDockerfileEnabled(docker));
    }

    @Test
    @DisplayName("Return true when dockerfile is null but app configuration exists")
    void isDockerfileEnabled_shouldReturnTrue_whenAppNotNull() {
        
        final DockerConfiguration docker = config(null, new ApplicationDockerConfiguration(), null);

        assertTrue(DockerUtils.isDockerfileEnabled(docker));
    }

    @Test
    @DisplayName("Return true when dockerfile is null but db configuration exists")
    void isDockerfileEnabled_shouldReturnTrue_whenDbNotNull() {
        
        final DockerConfiguration docker = config(null, null, new DbDockerConfiguration());

        assertTrue(DockerUtils.isDockerfileEnabled(docker));
    }

    @Test
    @DisplayName("Return false when nothing is configured (dockerfile == null, no app, no db)")
    void isDockerfileEnabled_shouldReturnFalse_whenAllNull() {
        
        final DockerConfiguration docker = config(null, null, null);

        assertFalse(DockerUtils.isDockerfileEnabled(docker));
    }
}
