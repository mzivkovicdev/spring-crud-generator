package com.markozivkovic.codegen.constants;

public class AdditionalConfigurationConstants {
    
    private AdditionalConfigurationConstants() {}

    public static final String GRAPHQL_SCALAR_CONFIG = "graphql.scalarConfig";
    public static final String REST_BASEPATH = "rest.basepath";

    public static final String OPT_LOCK_RETRY_CONFIGURATION = "optimisticLocking.retry.config";
    public static final String OPT_LOCK_MAX_ATTEMPTS = "optimisticLocking.retry.maxAttempts";
    public static final String OPT_LOCK_BACKOFF_DELAY_MS = "optimisticLocking.backoff.delayMs";
    public static final String OPT_LOCK_BACKOFF_MAX_DELAY_MS = "optimisticLocking.backoff.maxDelayMs";
    public static final String OPT_LOCK_BACKOFF_MULTIPLIER = "optimisticLocking.backoff.multiplier";

}
