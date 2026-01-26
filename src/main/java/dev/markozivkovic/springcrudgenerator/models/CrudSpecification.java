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

package dev.markozivkovic.springcrudgenerator.models;

import java.util.List;
import java.util.Objects;

public class CrudSpecification {
    
    private CrudConfiguration configuration;
    private List<ModelDefinition> entities;
    private PackageConfiguration packages;

    public CrudSpecification() {

    }

    public CrudSpecification(final CrudConfiguration configuration, final List<ModelDefinition> entities,
            final PackageConfiguration packages) {
        this.configuration = configuration;
        this.entities = entities;
        this.packages = packages;
    }

    public CrudConfiguration getConfiguration() {
        return this.configuration;
    }

    public CrudSpecification setConfiguration(final CrudConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    public List<ModelDefinition> getEntities() {
        return this.entities;
    }

    public CrudSpecification setEntities(final List<ModelDefinition> entities) {
        this.entities = entities;
        return this;
    }

    public PackageConfiguration getPackages() {
        return this.packages;
    }

    public CrudSpecification setPackages(final PackageConfiguration packages) {
        this.packages = packages;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CrudSpecification)) {
            return false;
        }
        final CrudSpecification crudSpecification = (CrudSpecification) o;
        return Objects.equals(configuration, crudSpecification.configuration) &&
                Objects.equals(entities, crudSpecification.entities) &&
                Objects.equals(packages, crudSpecification.packages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configuration, entities, packages);
    }

    @Override
    public String toString() {
        return "{" +
            " configuration='" + getConfiguration() + "'" +
            " entities='" + getEntities() + "'" +
            " packages='" + getPackages() + "'" +
            "}";
    }
    
}
