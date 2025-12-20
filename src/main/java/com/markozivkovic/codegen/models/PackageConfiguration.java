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

public class PackageConfiguration {
    
    private String annotations;
    private String businessservices;
    private String configurations;
    private String controllers;
    private String enums;
    private String exceptions;
    private String generated;
    private String mappers;
    private String models;
    private String resolvers;
    private String repositories;
    private String services;
    private String transferobjects;

    public PackageConfiguration() {}

    public PackageConfiguration(final String annotations, final String businessservices, final String configurations,
            final String controllers, final String enums, final String exceptions, final String generated,
            final String mappers, final String models, final String resolvers, final String repositories,
            final String services, final String transferobjects) {
        this.annotations = annotations;
        this.businessservices = businessservices;
        this.configurations = configurations;
        this.controllers = controllers;
        this.enums = enums;
        this.exceptions = exceptions;
        this.generated = generated;
        this.mappers = mappers;
        this.models = models;
        this.resolvers = resolvers;
        this.repositories = repositories;
        this.services = services;
        this.transferobjects = transferobjects;
    }

    public String getAnnotations() {
        return this.annotations;
    }

    public void setAnnotations(String annotations) {
        this.annotations = annotations;
    }

    public String getBusinessservices() {
        return this.businessservices;
    }

    public void setBusinessservices(String businessservices) {
        this.businessservices = businessservices;
    }

    public String getConfigurations() {
        return this.configurations;
    }

    public void setConfigurations(String configurations) {
        this.configurations = configurations;
    }

    public String getControllers() {
        return this.controllers;
    }

    public void setControllers(String controllers) {
        this.controllers = controllers;
    }

    public String getEnums() {
        return this.enums;
    }

    public void setEnums(String enums) {
        this.enums = enums;
    }

    public String getExceptions() {
        return this.exceptions;
    }

    public void setExceptions(String exceptions) {
        this.exceptions = exceptions;
    }

    public String getGenerated() {
        return this.generated;
    }

    public void setGenerated(String generated) {
        this.generated = generated;
    }

    public String getMappers() {
        return this.mappers;
    }

    public void setMappers(String mappers) {
        this.mappers = mappers;
    }

    public String getModels() {
        return this.models;
    }

    public void setModels(String models) {
        this.models = models;
    }

    public String getResolvers() {
        return this.resolvers;
    }

    public void setResolvers(String resolvers) {
        this.resolvers = resolvers;
    }

    public String getRepositories() {
        return this.repositories;
    }

    public void setRepositories(String repositories) {
        this.repositories = repositories;
    }

    public String getServices() {
        return this.services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getTransferobjects() {
        return this.transferobjects;
    }

    public void setTransferobjects(String transferobjects) {
        this.transferobjects = transferobjects;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PackageConfiguration)) {
            return false;
        }
        final PackageConfiguration packageConfiguration = (PackageConfiguration) o;
        return Objects.equals(annotations, packageConfiguration.annotations) &&
                Objects.equals(businessservices, packageConfiguration.businessservices) &&
                Objects.equals(configurations, packageConfiguration.configurations) &&
                Objects.equals(controllers, packageConfiguration.controllers) &&
                Objects.equals(enums, packageConfiguration.enums) &&
                Objects.equals(exceptions, packageConfiguration.exceptions) &&
                Objects.equals(generated, packageConfiguration.generated) &&
                Objects.equals(mappers, packageConfiguration.mappers) &&
                Objects.equals(models, packageConfiguration.models) &&
                Objects.equals(resolvers, packageConfiguration.resolvers) &&
                Objects.equals(repositories, packageConfiguration.repositories) &&
                Objects.equals(services, packageConfiguration.services) &&
                Objects.equals(transferobjects, packageConfiguration.transferobjects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                annotations, businessservices, configurations, controllers, enums, exceptions,
                generated, mappers, models, resolvers, repositories, services, transferobjects
        );
    }

    @Override
    public String toString() {
        return "{" +
            " annotations='" + getAnnotations() + "'" +
            ", businessservices='" + getBusinessservices() + "'" +
            ", configurations='" + getConfigurations() + "'" +
            ", controllers='" + getControllers() + "'" +
            ", enums='" + getEnums() + "'" +
            ", exceptions='" + getExceptions() + "'" +
            ", generated='" + getGenerated() + "'" +
            ", mappers='" + getMappers() + "'" +
            ", models='" + getModels() + "'" +
            ", resolvers='" + getResolvers() + "'" +
            ", repositories='" + getRepositories() + "'" +
            ", services='" + getServices() + "'" +
            ", transferobjects='" + getTransferobjects() + "'" +
            "}";
    }    

}
