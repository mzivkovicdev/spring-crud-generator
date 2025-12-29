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

package dev.markozivkovic.codegen.generators.tests;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.codegen.generators.CodeGenerator;
import dev.markozivkovic.codegen.models.CrudConfiguration;
import dev.markozivkovic.codegen.models.ModelDefinition;
import dev.markozivkovic.codegen.models.PackageConfiguration;

public class SpringCrudTestGenerator implements CodeGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCrudTestGenerator.class);

    private static final String JPA_SERVICE_TEST = "jpa-service-test";
    private static final String BUSINESS_SERVICE_TEST = "business-service-test";
    private static final String MAPPER_TEST = "mapper-test";
    private static final String CONTROLLER_TEST = "controller-test";
    private static final String GRAPHQL_TEST = "graphql-test";

    private final Map<String, CodeGenerator> GENERATORS;

    public SpringCrudTestGenerator(final CrudConfiguration configuration, final List<ModelDefinition> entites,
                final PackageConfiguration packageConfiguration) {
        this.GENERATORS = Map.ofEntries(
            Map.entry(JPA_SERVICE_TEST, new ServiceUnitTestGenerator(configuration, entites, packageConfiguration)),
            Map.entry(BUSINESS_SERVICE_TEST, new BusinessServiceUnitTestGenerator(configuration, entites, packageConfiguration)),
            Map.entry(MAPPER_TEST, new MapperUnitTestGenerator(configuration, entites, packageConfiguration)),
            Map.entry(CONTROLLER_TEST, new RestControllerUnitTestGenerator(configuration, entites, packageConfiguration)),
            Map.entry(GRAPHQL_TEST, new GraphQlUnitTestGenerator(configuration, entites, packageConfiguration))
        );
    }

    @Override
    public void generate(final ModelDefinition modelDefinition, final String outputDir) {
        LOGGER.info("Test generator started for model: {}", modelDefinition.getName());

        this.GENERATORS.values().forEach(generator -> generator.generate(modelDefinition, outputDir));

        LOGGER.info("Test generator finished for model: {}", modelDefinition.getName());
    }
    
}
