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

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Validation constraints that can be attached to a field.
 *
 * Supported constraints:
 * - required: field must be present / not null
 * - notBlank: only for String (not null, not empty, not whitespace)
 * - notEmpty: for String, collections, maps (not null, size > 0)
 * - minLength / maxLength: String length constraints
 * - min / max: numeric value constraints
 * - minItems / maxItems: collection size constraints
 * - pattern: String regex (either inline regex or reference key)
 * - email: String must be a valid email
 */
public class ValidationDefinition {
    
    /**
     * Field must be present / not null.
     */
    private Boolean required;

    /**
     * Only for String: must not be null and must contain at least one non-whitespace character.
     */
    private Boolean notBlank;

    /**
     * For String/collections/maps: must not be null and must not be empty.
     */
    private Boolean notEmpty;

    /**
     * For String: minimum length.
     */
    private Integer minLength;

    /**
     * For String: maximum length.
     */
    private Integer maxLength;

    /**
     * For numeric types: minimum value (integer/decimal).
     */
    private BigDecimal min;

    /**
     * For numeric types: maximum value.
     */
    private BigDecimal max;

    /**
     * For collections: minimum number of items.
     */
    private Integer minItems;

    /**
     * For collections: maximum number of items.
     */
    private Integer maxItems;

    /**
     * For String: regex constraint.
     */
    private String pattern;

    /**
     * For String: email format.
     */
    private Boolean email;

    public ValidationDefinition() {

    }

    public ValidationDefinition(final Boolean required, final Boolean notBlank, final Boolean notEmpty, final Integer minLength,
                               final Integer maxLength, final BigDecimal min, final BigDecimal max, final Integer minItems,
                               final Integer maxItems, final String pattern, final Boolean email) {
        this.required = required;
        this.notBlank = notBlank;
        this.notEmpty = notEmpty;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.min = min;
        this.max = max;
        this.minItems = minItems;
        this.maxItems = maxItems;
        this.pattern = pattern;
        this.email = email;
    }

    public Boolean isRequired() {
        return this.required;
    }

    public Boolean getRequired() {
        return this.required;
    }

    public ValidationDefinition setRequired(final Boolean required) {
        this.required = required;
        return this;
    }

    public Boolean isNotBlank() {
        return this.notBlank;
    }

    public Boolean getNotBlank() {
        return this.notBlank;
    }

    public ValidationDefinition setNotBlank(final Boolean notBlank) {
        this.notBlank = notBlank;
        return this;
    }

    public Boolean isNotEmpty() {
        return this.notEmpty;
    }

    public Boolean getNotEmpty() {
        return this.notEmpty;
    }

    public ValidationDefinition setNotEmpty(final Boolean notEmpty) {
        this.notEmpty = notEmpty;
        return this;
    }

    public Integer getMinLength() {
        return this.minLength;
    }

    public ValidationDefinition setMinLength(final Integer minLength) {
        this.minLength = minLength;
        return this;
    }

    public Integer getMaxLength() {
        return this.maxLength;
    }

    public ValidationDefinition setMaxLength(final Integer maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public BigDecimal getMin() {
        return this.min;
    }

    public ValidationDefinition setMin(final BigDecimal min) {
        this.min = min;
        return this;
    }

    public BigDecimal getMax() {
        return this.max;
    }

    public ValidationDefinition setMax(final BigDecimal max) {
        this.max = max;
        return this;
    }

    public Integer getMinItems() {
        return this.minItems;
    }

    public ValidationDefinition setMinItems(final Integer minItems) {
        this.minItems = minItems;
        return this;
    }

    public Integer getMaxItems() {
        return this.maxItems;
    }

    public ValidationDefinition setMaxItems(final Integer maxItems) {
        this.maxItems = maxItems;
        return this;
    }

    public String getPattern() {
        return this.pattern;
    }

    public ValidationDefinition setPattern(final String pattern) {
        this.pattern = pattern;
        return this;
    }

    public Boolean isEmail() {
        return this.email;
    }

    public Boolean getEmail() {
        return this.email;
    }

    public ValidationDefinition setEmail(final Boolean email) {
        this.email = email;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ValidationDefinition)) {
            return false;
        }
        final ValidationDefinition validationDefinition = (ValidationDefinition) o;
        return Objects.equals(required, validationDefinition.required) &&
                Objects.equals(notBlank, validationDefinition.notBlank) &&
                Objects.equals(notEmpty, validationDefinition.notEmpty) &&
                Objects.equals(minLength, validationDefinition.minLength) &&
                Objects.equals(maxLength, validationDefinition.maxLength) &&
                Objects.equals(min, validationDefinition.min) &&
                Objects.equals(max, validationDefinition.max) &&
                Objects.equals(minItems, validationDefinition.minItems) &&
                Objects.equals(maxItems, validationDefinition.maxItems) &&
                Objects.equals(pattern, validationDefinition.pattern) &&
                Objects.equals(email, validationDefinition.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                required, notBlank, notEmpty, minLength, maxLength, min, max, minItems,
                maxItems, pattern, email
    );
    }

    @Override
    public String toString() {
        return "{" +
            " required='" + isRequired() + "'" +
            ", notBlank='" + isNotBlank() + "'" +
            ", notEmpty='" + isNotEmpty() + "'" +
            ", minLength='" + getMinLength() + "'" +
            ", maxLength='" + getMaxLength() + "'" +
            ", min='" + getMin() + "'" +
            ", max='" + getMax() + "'" +
            ", minItems='" + getMinItems() + "'" +
            ", maxItems='" + getMaxItems() + "'" +
            ", pattern='" + getPattern() + "'" +
            ", email='" + isEmail() + "'" +
            "}";
    }    

}
