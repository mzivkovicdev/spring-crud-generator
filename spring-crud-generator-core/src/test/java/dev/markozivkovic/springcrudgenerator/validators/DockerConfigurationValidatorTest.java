package dev.markozivkovic.springcrudgenerator.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.ApplicationDockerConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DbDockerConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.DockerConfiguration;

class DockerConfigurationValidatorTest {

    @Test
    @DisplayName("Should do nothing when docker configuration is null")
    void validate_nullDockerConfiguration_doesNothing() {
        assertDoesNotThrow(() -> DockerConfigurationValidator.validate(null));
    }

    @Test
    @DisplayName("Should allow dockerfile=false when app and db are null")
    void validate_dockerfileFalseAndNoAppNoDb_doesNothing() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(false);
        docker.setApp(null);
        docker.setDb(null);

        assertDoesNotThrow(() -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should throw when dockerfile=false and app is set")
    void validate_dockerfileFalseWithAppSet_throwsIllegalArgumentException() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(false);

        final ApplicationDockerConfiguration app = new ApplicationDockerConfiguration();
        app.setPort(8080);
        docker.setApp(app);

        docker.setDb(null);

        assertThrows(IllegalArgumentException.class,
                () -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should throw when dockerfile=false and db is set")
    void validate_dockerfileFalseWithDbSet_throwsIllegalArgumentException() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(false);
        docker.setApp(null);

        final DbDockerConfiguration db = new DbDockerConfiguration();
        db.setPort(5432);
        docker.setDb(db);

        assertThrows(IllegalArgumentException.class,
                () -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should throw when dockerfile=false and both app and db are set")
    void validate_dockerfileFalseWithAppAndDbSet_throwsIllegalArgumentException() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(false);

        final ApplicationDockerConfiguration app = new ApplicationDockerConfiguration();
        app.setPort(8080);
        docker.setApp(app);

        final DbDockerConfiguration db = new DbDockerConfiguration();
        db.setPort(5432);
        docker.setDb(db);

        assertThrows(IllegalArgumentException.class,
                () -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should allow dockerfile=true with null app and db")
    void validate_dockerfileTrueWithNoAppNoDb_doesNothing() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(true);
        docker.setApp(null);
        docker.setDb(null);

        assertDoesNotThrow(() -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should allow valid app port (within 1-65535)")
    void validate_validAppPort_doesNothing() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(true);

        final ApplicationDockerConfiguration app = new ApplicationDockerConfiguration();
        app.setPort(8080);
        docker.setApp(app);

        docker.setDb(null);

        assertDoesNotThrow(() -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should allow null app port")
    void validate_nullAppPort_doesNothing() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(true);

        final ApplicationDockerConfiguration app = new ApplicationDockerConfiguration();
        app.setPort(null);
        docker.setApp(app);

        docker.setDb(null);

        assertDoesNotThrow(() -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should throw when app port is < 1")
    void validate_appPortLessThanOne_throwsIllegalArgumentException() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(true);

        final ApplicationDockerConfiguration app = new ApplicationDockerConfiguration();
        app.setPort(0);
        docker.setApp(app);

        docker.setDb(null);

        assertThrows(IllegalArgumentException.class,
                () -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should throw when app port is > 65535")
    void validate_appPortGreaterThan65535_throwsIllegalArgumentException() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(true);

        final ApplicationDockerConfiguration app = new ApplicationDockerConfiguration();
        app.setPort(65536);
        docker.setApp(app);

        docker.setDb(null);

        assertThrows(IllegalArgumentException.class,
                () -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should allow valid db port (within 1-65535)")
    void validate_validDbPort_doesNothing() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(true);
        docker.setApp(null);

        final DbDockerConfiguration db = new DbDockerConfiguration();
        db.setPort(5432);
        docker.setDb(db);

        assertDoesNotThrow(() -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should allow null db port")
    void validate_nullDbPort_doesNothing() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(true);
        docker.setApp(null);

        final DbDockerConfiguration db = new DbDockerConfiguration();
        db.setPort(null);
        docker.setDb(db);

        assertDoesNotThrow(() -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should throw when db port is < 1")
    void validate_dbPortLessThanOne_throwsIllegalArgumentException() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(true);
        docker.setApp(null);

        final DbDockerConfiguration db = new DbDockerConfiguration();
        db.setPort(0);
        docker.setDb(db);

        assertThrows(IllegalArgumentException.class,
                () -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should throw when db port is > 65535")
    void validate_dbPortGreaterThan65535_throwsIllegalArgumentException() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(true);
        docker.setApp(null);

        final DbDockerConfiguration db = new DbDockerConfiguration();
        db.setPort(65536);
        docker.setDb(db);

        assertThrows(IllegalArgumentException.class,
                () -> DockerConfigurationValidator.validate(docker));
    }

    @Test
    @DisplayName("Should allow valid app and db ports together")
    void validate_validAppAndDbPorts_doesNothing() {
        final DockerConfiguration docker = new DockerConfiguration();
        docker.setDockerfile(true);

        final ApplicationDockerConfiguration app = new ApplicationDockerConfiguration();
        app.setPort(8080);
        docker.setApp(app);

        final DbDockerConfiguration db = new DbDockerConfiguration();
        db.setPort(5432);
        docker.setDb(db);

        assertDoesNotThrow(() -> DockerConfigurationValidator.validate(docker));
    }
}
