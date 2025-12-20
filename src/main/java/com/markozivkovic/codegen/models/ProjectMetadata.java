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
