package com.markozivkovic.codegen.constants;

public class GeneratorConstants {
    
    private GeneratorConstants() {}

    public static final String SRC_MAIN_RESOURCES_GRAPHQL = "src/main/resources/graphql";
    
    public static final class Transaction {
        private Transaction() {}
        public static final String OPTIMISTIC_LOCKING_RETRY = "OptimisticLockingRetry";
        public static final String OPTIMISTIC_LOCKING_RETRY_ANNOTATION = "@OptimisticLockingRetry";
    }

    public final class DefaultPackageLayout {
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
        public static final String GRAPHQL = "graphql";
        public static final String CONTROLLERS = "controllers";
        public static final String SERVICES = "services";
        public static final String ENUMS = "enums";
        public static final String EXCEPTIONS = "exceptions";
        public static final String RESPONSES = "responses";
        public static final String HANDLERS = "handlers";
        public static final String REPOSITORIES = "repositories";
    }

}
