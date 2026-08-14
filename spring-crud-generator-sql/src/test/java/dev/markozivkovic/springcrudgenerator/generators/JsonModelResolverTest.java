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

package dev.markozivkovic.springcrudgenerator.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.markozivkovic.springcrudgenerator.models.FieldDefinition;
import dev.markozivkovic.springcrudgenerator.models.ModelDefinition;

class JsonModelResolverTest {

    @Test
    void resolveReferencedModels_whenJsonUsesBasicTypes_returnsEmptyList() {
        final ModelDefinition sourceModel = model(
                "OrderEntity",
                field("metadata", "JSON<String>"),
                field("identifiers", "JSON<List<Long>>"));

        final List<ModelDefinition> jsonModels = JsonModelResolver.resolveReferencedModels(
                List.of(sourceModel),
                List.of(sourceModel));

        assertEquals(List.of(), jsonModels);
    }

    @Test
    void resolveReferencedModels_whenJsonReferencesActiveModel_returnsModelOnce() {
        final ModelDefinition addressModel = model("Address", field("street", "String"));
        final ModelDefinition sourceModel = model(
                "OrderEntity",
                field("billingAddress", "JSON<Address>"),
                field("shippingAddresses", "JSON<List<Address>>"));

        final List<ModelDefinition> jsonModels = JsonModelResolver.resolveReferencedModels(
                List.of(sourceModel),
                List.of(sourceModel, addressModel));

        assertEquals(1, jsonModels.size());
        assertSame(addressModel, jsonModels.get(0));
    }

    @Test
    void resolveReferencedModels_whenJsonModelIsMissing_throwsIllegalArgumentException() {
        final ModelDefinition sourceModel = model(
                "OrderEntity",
                field("address", "JSON<Address>"));

        assertThrows(
                IllegalArgumentException.class,
                () -> JsonModelResolver.resolveReferencedModels(
                        List.of(sourceModel),
                        List.of(sourceModel)));
    }

    private static FieldDefinition field(final String name, final String type) {
        return new FieldDefinition()
                .setName(name)
                .setType(type);
    }

    private static ModelDefinition model(final String name, final FieldDefinition... fields) {
        return new ModelDefinition()
                .setName(name)
                .setFields(List.of(fields));
    }
}
