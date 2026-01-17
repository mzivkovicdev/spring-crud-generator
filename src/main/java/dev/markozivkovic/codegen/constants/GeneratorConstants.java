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

package dev.markozivkovic.codegen.constants;

public class GeneratorConstants {
    
    private GeneratorConstants() {}

    public static final String PAGE_TO = "PageTO";
    public static final String GLOBAL_GRAPHQL_EXCEPTION_HANDLER = "GlobalGraphQlExceptionHandler";
    public static final String GLOBAL_REST_EXCEPTION_HANDLER = "GlobalRestExceptionHandler";
    public static final String SRC_MAIN_RESOURCES = "src/main/resources";
    public static final String SRC_MAIN_RESOURCES_GRAPHQL = "src/main/resources/graphql";
    public static final String SRC_MAIN_RESOURCES_DB_MIGRATION = "src/main/resources/db/migration";
    public static final String SRC_MAIN_RESOURCES_SWAGGER = "src/main/resources/swagger";
    public static final String OPEN_API_GENERATOR_IGNORE = ".openapi-generator-ignore";
    
    public static final class Transaction {
        private Transaction() {}
        public static final String OPTIMISTIC_LOCKING_RETRY = "OptimisticLockingRetry";
        public static final String OPTIMISTIC_LOCKING_RETRY_ANNOTATION = "@OptimisticLockingRetry";
    }

    public static final class DefaultPackageLayout {
        private DefaultPackageLayout() {}
        public static final String ANNOTATIONS = "annotations";
        public static final String BUSINESS_SERVICES = "businessservices";
        public static final String CONFIGURATIONS = "configurations";
        public static final String RESOLVERS = "resolvers";
        public static final String MAPPERS = "mappers";
        public static final String MODELS = "models";
        public static final String MODEL = "model";
        public static final String TRANSFEROBJECTS = "transferobjects";
        public static final String HELPERS = "helpers";
        public static final String REST = "rest";
        public static final String GENERATED = "generated";
        public static final String API = "api";
        public static final String GRAPHQL = "graphql";
        public static final String CONTROLLERS = "controllers";
        public static final String SERVICES = "services";
        public static final String ENUMS = "enums";
        public static final String EXCEPTIONS = "exceptions";
        public static final String RESPONSES = "responses";
        public static final String HANDLERS = "handlers";
        public static final String REPOSITORIES = "repositories";
        public static final String SWAGGER = "swagger";
    }

    public static final class GeneratorContextKeys {
        private GeneratorContextKeys() {}
        public static final String GRAPHQL_CONFIGURATION = "graphql-configuration";
        public static final String GRAPHQL_DATE_TIME_CONFIGURATION = "graphql-date-time-configuration";
        public static final String ADDITIONAL_CONFIG = "additionalConfig";
        public static final String CACHE_CONFIGURATION = "cacheConfiguration";
        public static final String OPTIMISTIC_LOCKING_RETRY = "optimisticLockingRetry";
        public static final String RETRYABLE_ANNOTATION = "retryableAnnotation";
        public static final String DOCKER_FILE = "dockerfile";
        public static final String DOCKER_COMPOSE = "docker-compose";
        public static final String EXCEPTIONS = "exceptions";
        public static final String GRAPHQL = "graphql";
        public static final String MIGRATION_SCRIPT = "migration-script";
        public static final String OPENAPI_CODEGEN = "openapi-codegen";
        public static final String SWAGGER = "swagger";
        public static final String RESOLVER_TEST_CONFIG = "resolver-test-config";
        public static final String JPA_AUDITING_CONFIG = "jpa-auditing-config";
    }

}
