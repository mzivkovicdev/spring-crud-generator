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

package dev.markozivkovic.springcrudgenerator.deserializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import dev.markozivkovic.springcrudgenerator.models.IdDefinition;

/**
 * Supports two YAML/JSON forms for field.id:
 * - boolean true: lightweight ID marker
 * - object: SQL/JPA ID configuration
 */
public class IdDefinitionDeserializer extends StdDeserializer<IdDefinition> {

    public IdDefinitionDeserializer() {
        super(IdDefinition.class);
    }

    @Override
    public IdDefinition deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {

        final JsonToken token = parser.currentToken();

        if (JsonToken.VALUE_TRUE.equals(token)) {
            return new IdDefinition().setMarkerOnly(true);
        }

        if (JsonToken.VALUE_FALSE.equals(token) || JsonToken.VALUE_NULL.equals(token)) {
            return null;
        }

        if (JsonToken.START_OBJECT.equals(token)) {
            final JsonNode node = parser.readValueAsTree();
            final IdDefinition idDefinition = parser.getCodec().treeToValue(node, IdDefinition.class);
            return idDefinition == null ? null : idDefinition.setMarkerOnly(false);
        }

        return (IdDefinition) context.handleUnexpectedToken(IdDefinition.class, parser);
    }
}
