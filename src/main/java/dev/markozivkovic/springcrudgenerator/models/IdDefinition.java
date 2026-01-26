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

import java.util.Objects;

public class IdDefinition {
    
    private IdStrategyEnum strategy;
    private String generatorName;
    private Integer allocationSize;
    private Integer initialValue;
    private String pkColumnName;
    private String valueColumnName;

    public IdDefinition() {

    }

    public IdDefinition(final IdStrategyEnum strategy, final String generatorName,
            final Integer allocationSize, final Integer initialValue) {
        this.strategy = strategy;
        this.generatorName = generatorName;
        this.allocationSize = allocationSize;
        this.initialValue = initialValue;
    }

    public IdStrategyEnum getStrategy() {
        return this.strategy;
    }

    public IdDefinition setStrategy(final IdStrategyEnum strategy) {
        this.strategy = strategy;
        return this;
    }

    public String getGeneratorName() {
        return this.generatorName;
    }

    public IdDefinition setGeneratorName(final String generatorName) {
        this.generatorName = generatorName;
        return this;
    }

    public Integer getAllocationSize() {
        return this.allocationSize;
    }

    public IdDefinition setAllocationSize(final Integer allocationSize) {
        this.allocationSize = allocationSize;
        return this;
    }

    public Integer getInitialValue() {
        return this.initialValue;
    }

    public IdDefinition setInitialValue(final Integer initialValue) {
        this.initialValue = initialValue;
        return this;
    }

    public String getPkColumnName() {
        return pkColumnName;
    }

    public IdDefinition setPkColumnName(String pkColumnName) {
        this.pkColumnName = pkColumnName;
        return this;
    }

    public String getValueColumnName() {
        return valueColumnName;
    }

    public IdDefinition setValueColumnName(String valueColumnName) {
        this.valueColumnName = valueColumnName;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IdDefinition)) {
            return false;
        }
        final IdDefinition idGenerationDefinition = (IdDefinition) o;
        return Objects.equals(strategy, idGenerationDefinition.strategy) &&
                Objects.equals(generatorName, idGenerationDefinition.generatorName) &&
                Objects.equals(allocationSize, idGenerationDefinition.allocationSize) &&
                Objects.equals(initialValue, idGenerationDefinition.initialValue) &&
                Objects.equals(pkColumnName, idGenerationDefinition.pkColumnName) &&
                Objects.equals(valueColumnName, idGenerationDefinition.valueColumnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                strategy, generatorName, allocationSize, initialValue, pkColumnName, valueColumnName
        );
    }

    @Override
    public String toString() {
        return "{" +
            " strategy='" + getStrategy() + "'" +
            ", generatorName='" + getGeneratorName() + "'" +
            ", allocationSize='" + getAllocationSize() + "'" +
            ", initialValue='" + getInitialValue() + "'" +
            ", pkColumnName='" + getPkColumnName() + "'" +
            ", valueColumnName='" + getValueColumnName() + "'" +
            "}";
    }    

    public enum IdStrategyEnum {
        TABLE,
        SEQUENCE,
        UUID,
        IDENTITY,
        AUTO
    }
}
