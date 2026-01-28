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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.markozivkovic.springcrudgenerator.utils.FieldUtils;

public class FieldDefinition {
    
    private String name;
    private String type;
    private String description;
    private IdDefinition id;
    private List<String> values = new ArrayList<>();
    private RelationDefinition relation;
    private ColumnDefinition column;
    private ValidationDefinition validation;

    public FieldDefinition() {

    }

    public FieldDefinition(final String name, final String type, final String description,
            final IdDefinition id, final List<String> values, final RelationDefinition relation,
            final ColumnDefinition column, final ValidationDefinition validation) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.id = id;
        this.values = values;
        this.relation = relation;
        this.column = column;
        this.validation = validation;
    }

    public String getName() {
        return this.name;
    }

    public FieldDefinition setName(final String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return this.type;
    }

    public FieldDefinition setType(final String type) {
        this.type = type;
        return this;
    }

    public String getDescription() {
        return this.description;
    }

    public FieldDefinition setDescription(final String description) {
        this.description = description;
        return this;
    }

    public IdDefinition getId() {
        return this.id;
    }

    public FieldDefinition setId(final IdDefinition id) {
        this.id = id;
        return this;
    }

    public List<String> getValues() {
        return this.values;
    }

    public FieldDefinition setValues(final List<String> values) {
        this.values = values;
        return this;
    }

    public String getResolvedType() {
        return FieldUtils.computeResolvedType(this);
    }

    public RelationDefinition getRelation() {
        return this.relation;
    }

    public FieldDefinition setRelation(final RelationDefinition relation) {
        this.relation = relation;
        return this;
    }

    public ColumnDefinition getColumn() {
        return this.column;
    }

    public FieldDefinition setColumn(final ColumnDefinition column) {
        this.column = column;
        return this;
    }

    public ValidationDefinition getValidation() {
        return this.validation;
    }

    public FieldDefinition setValidation(final ValidationDefinition validation) {
        this.validation = validation;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FieldDefinition)) {
            return false;
        }
        final FieldDefinition fieldDefinition = (FieldDefinition) o;
        return Objects.equals(name, fieldDefinition.name) &&
                Objects.equals(type, fieldDefinition.type) &&
                Objects.equals(description, fieldDefinition.description) &&
                id == fieldDefinition.id &&
                Objects.equals(values, fieldDefinition.values) &&
                Objects.equals(relation, fieldDefinition.relation) &&
                Objects.equals(column, fieldDefinition.column) &&
                Objects.equals(validation, fieldDefinition.validation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, description, id, values, relation, column, validation);
    }

    @Override
    public String toString() {
        return "{" +
            " name='" + getName() + "'" +
            ", type='" + getType() + "'" +
            ", description='" + getDescription() + "'" +
            ", id='" + getId() + "'" +
            ", values='" + getValues() + "'" +
            ", relation='" + getRelation() + "'" +
            ", column='" + getColumn() + "'" +
            ", validation='" + getValidation() + "'" +
            "}";
    }

}
