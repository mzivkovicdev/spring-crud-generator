package com.markozivkovic.codegen.models;

import java.util.Objects;

public class ProjectMetadata {
    
    private final String artifactId;
    private final String version;
    private final String projectBaseDir;

    public ProjectMetadata(final String artifactId, final String version, final String projectBaseDir) {
        this.artifactId = artifactId;
        this.version = version;
        this.projectBaseDir = projectBaseDir;
    }

    public String getArtifactId() {
        return this.artifactId;
    }

    public String getVersion() {
        return this.version;
    }

    public String getProjectBaseDir() {
        return this.projectBaseDir;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ProjectMetadata)) {
            return false;
        }
        final ProjectMetadata projectMetadata = (ProjectMetadata) o;
        return Objects.equals(artifactId, projectMetadata.artifactId) &&
                Objects.equals(version, projectMetadata.version) &&
                Objects.equals(projectBaseDir, projectMetadata.projectBaseDir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactId, version, projectBaseDir);
    }

    @Override
    public String toString() {
        return "{" +
            " artifactId='" + getArtifactId() + "'" +
            ", version='" + getVersion() + "'" +
            ", projectBaseDir='" + getProjectBaseDir() + "'" +
            "}";
    }    

}
