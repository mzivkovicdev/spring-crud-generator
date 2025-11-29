package com.markozivkovic.codegen.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GeneratorState {
    
    private String generatorVersion;
    private String configuration;
    private List<ModelState> models = new ArrayList<>();

    public GeneratorState() {}

    public GeneratorState(final String generatorVersion, final String configuration,
                final List<ModelState> models) {
        this.generatorVersion = generatorVersion;
        this.configuration = configuration;
        this.models = models;
    }

    public String getGeneratorVersion() {
        return this.generatorVersion;
    }

    public GeneratorState setGeneratorVersion(final String generatorVersion) {
        this.generatorVersion = generatorVersion;
        return this;
    }

    public String getConfiguration() {
        return this.configuration;
    }

    public GeneratorState setConfiguration(final String configuration) {
        this.configuration = configuration;
        return this;
    }

    public List<ModelState> getModels() {
        return this.models;
    }

    public GeneratorState setModels(final List<ModelState> models) {
        this.models = models;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GeneratorState)) {
            return false;
        }
        final GeneratorState generatorState = (GeneratorState) o;
        return Objects.equals(generatorVersion, generatorState.generatorVersion) &&
                Objects.equals(configuration, generatorState.configuration) &&
                Objects.equals(models, generatorState.models);
    }

    @Override
    public int hashCode() {
        return Objects.hash(generatorVersion, configuration, models);
    }

    @Override
    public String toString() {
        return "{" +
            " generatorVersion='" + getGeneratorVersion() + "'" +
            ", configuration='" + getConfiguration() + "'" +
            ", models='" + getModels() + "'" +
            "}";
    }

    public static class ModelState {

        private String name;
        private String fingerprint;

        public ModelState() {}

        public ModelState(final String name, final String fingerprint) {
            this.name = name;
            this.fingerprint = fingerprint;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFingerprint() {
            return this.fingerprint;
        }

        public void setFingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
        }

        public ModelState name(String name) {
            this.name = name;
            return this;
        }

        public ModelState fingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof ModelState)) {
                return false;
            }
            final ModelState modelState = (ModelState) o;
            return Objects.equals(name, modelState.name) &&
                    Objects.equals(fingerprint, modelState.fingerprint);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, fingerprint);
        }

        @Override
        public String toString() {
            return "{" +
                " name='" + getName() + "'" +
                ", fingerprint='" + getFingerprint() + "'" +
                "}";
        }
        
    }

}
