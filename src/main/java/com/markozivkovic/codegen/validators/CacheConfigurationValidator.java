package com.markozivkovic.codegen.validators;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.markozivkovic.codegen.models.CrudConfiguration.CacheConfiguration;
import com.markozivkovic.codegen.models.CrudConfiguration.CacheConfiguration.CacheTypeEnum;

public class CacheConfigurationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfigurationValidator.class);

    private CacheConfigurationValidator() {}

    /**
     * Validates the cache configuration.
     * 
     * Checks if the cache configuration is valid according to the following rules:
     * - If cache.enabled is set to false, but cache.type, cache.maxSize or cache.expiration are set, an {@link IllegalArgumentException} is thrown.
     * - If cache.enabled is not set, but cache.type, cache.maxSize or cache.expiration are set, a warning is logged and cache.enabled is set to true.
     * - If cache.enabled is set to true, but no cache.type, cache.maxSize or cache.expiration are provided, a warning is logged and the cache type is set to SIMPLE.
     * - If cache.type is set to CAFFEINE, cache.maxSize must be > 0, otherwise an {@link IllegalArgumentException} is thrown.
     * - If cache.type is not set to CAFFEINE, cache.maxSize is ignored and a warning is logged.
     * - If cache.expiration is set to <= 0, an {@link IllegalArgumentException} is thrown.
     * 
     * @param cacheConfiguration the cache configuration to validate
     * @throws IllegalArgumentException if the cache configuration is invalid
     */
    public static void validate(final CacheConfiguration cacheConfiguration) {

        if (Objects.isNull(cacheConfiguration)) {
            return;
        }
        
        final Boolean enabled = cacheConfiguration.getEnabled();
        final Boolean hasParams = Objects.nonNull(cacheConfiguration.getExpiration()) ||
                Objects.nonNull(cacheConfiguration.getMaxSize()) ||
                Objects.nonNull(cacheConfiguration.getType());

        if (Boolean.FALSE.equals(enabled) && hasParams) {
            throw new IllegalArgumentException(
                """
                Invalid cache configuration: cache.enabled is set to false, but cache.type, cache.maxSize or cache.expiration are set.
                Please set cache.enabled to true or just remove cache.type, cache.maxSize and cache.expiration.
                        """
            );
        }

        if (Objects.isNull(enabled)) {
            if (!hasParams) {
                return;
            } else {
                LOGGER.warn("Cache parameters are provided without setting cache.enabled. Implicitly treating cache.enabled=true.");
            }
        }

        if (Boolean.TRUE.equals(enabled) && !hasParams) {
            LOGGER.warn("cache.enabled=true, but no cache.type/maxSize/expiration are provided. Using SIMPLE cache.");
            return;
        }

        if (CacheTypeEnum.CAFFEINE.equals(cacheConfiguration.getType())) {
            if (cacheConfiguration.getMaxSize() != null && cacheConfiguration.getMaxSize() <= 0) {
                throw new IllegalArgumentException(
                    String.format(
                        "cache.maxSize must be > 0 for %s cache.",
                        CacheTypeEnum.CAFFEINE.name()
                    )
                );
            }
        } else {
            if (Objects.nonNull(cacheConfiguration.getMaxSize())) {
                LOGGER.warn(
                    "cache.maxSize is ignored for {} cache.",
                    Objects.nonNull(cacheConfiguration.getType()) ? cacheConfiguration.getType() : CacheTypeEnum.SIMPLE
                );
            }
        }

        if (Objects.nonNull(cacheConfiguration.getExpiration()) && cacheConfiguration.getExpiration() <= 0) {
            throw new IllegalArgumentException(
                String.format(
                    "cache.expiration must be > 0."
                )
            );
        }
    }
}