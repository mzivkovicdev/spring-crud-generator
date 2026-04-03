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

import static dev.markozivkovic.springcrudgenerator.constants.ImportConstants.PACKAGE;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.markozivkovic.springcrudgenerator.constants.GeneratorConstants;
import dev.markozivkovic.springcrudgenerator.constants.TemplateContextConstants;
import dev.markozivkovic.springcrudgenerator.context.GeneratorContext;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.RateLimitDefinition;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.RateLimitResponseConfig;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.RateLimitingConfiguration;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.RateLimitingConfiguration.KeyStrategyEnum;
import dev.markozivkovic.springcrudgenerator.models.CrudConfiguration.RateLimitingConfiguration.RateLimitTypeEnum;
import dev.markozivkovic.springcrudgenerator.models.PackageConfiguration;
import dev.markozivkovic.springcrudgenerator.utils.FileWriterUtils;
import dev.markozivkovic.springcrudgenerator.utils.FreeMarkerTemplateProcessorUtils;
import dev.markozivkovic.springcrudgenerator.utils.PackageUtils;
import dev.markozivkovic.springcrudgenerator.utils.SpringBootVersionUtils;

public class RateLimitingGenerator implements ProjectArtifactGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitingGenerator.class);

    private static final long DEFAULT_CAPACITY = 100L;
    private static final long DEFAULT_REFILL_TOKENS = 100L;
    private static final long DEFAULT_REFILL_DURATION = 60L;
    private static final int DEFAULT_STATUS_CODE = 429;
    private static final String DEFAULT_MESSAGE = "Rate limit exceeded. Please try again later.";

    private final CrudConfiguration crudConfiguration;
    private final PackageConfiguration packageConfiguration;

    public RateLimitingGenerator(final CrudConfiguration crudConfiguration,
            final PackageConfiguration packageConfiguration) {
        this.crudConfiguration = crudConfiguration;
        this.packageConfiguration = packageConfiguration;
    }

    @Override
    public void generate(final String outputDir) {

        if (Objects.isNull(crudConfiguration.getRateLimiting())
                || !Boolean.TRUE.equals(crudConfiguration.getRateLimiting().getEnabled())) {
            LOGGER.info("Skipping RateLimitingGenerator, as rate limiting is not enabled.");
            return;
        }

        if (GeneratorContext.isGenerated(GeneratorConstants.GeneratorContextKeys.RATE_LIMITING_CONFIGURATION)) {
            return;
        }

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        final String configSubPackage = PackageUtils.computeConfigurationSubPackage(packageConfiguration);
        final String configPackage = PackageUtils.computeConfigurationPackage(packagePath, packageConfiguration);
        final boolean isSpringBoot3 = SpringBootVersionUtils.isSpringBoot3(crudConfiguration.getSpringBootVersion());

        final RateLimitingConfiguration rl = crudConfiguration.getRateLimiting();
        final RateLimitTypeEnum type = Objects.nonNull(rl.getType()) ? rl.getType() : RateLimitTypeEnum.IN_MEMORY;
        final KeyStrategyEnum keyStrategy = Objects.nonNull(rl.getKeyStrategy()) ? rl.getKeyStrategy() : KeyStrategyEnum.IP;
        final String keyHeader = Objects.nonNull(rl.getKeyHeader()) ? rl.getKeyHeader() : "X-Client-Id";

        final RateLimitDefinition global = Objects.nonNull(rl.getGlobal()) ? rl.getGlobal() : new RateLimitDefinition();
        final long capacity = Objects.nonNull(global.getCapacity()) ? global.getCapacity() : DEFAULT_CAPACITY;
        final long refillTokens = Objects.nonNull(global.getRefillTokens()) ? global.getRefillTokens() : DEFAULT_REFILL_TOKENS;
        final long refillDuration = Objects.nonNull(global.getRefillDuration()) ? global.getRefillDuration() : DEFAULT_REFILL_DURATION;
        final boolean hasOverdraft = Objects.nonNull(global.getOverdraft());

        final RateLimitResponseConfig responseConfig = rl.getResponse();
        final int statusCode = Objects.nonNull(responseConfig) && Objects.nonNull(responseConfig.getStatusCode())
                ? responseConfig.getStatusCode() : DEFAULT_STATUS_CODE;
        final boolean includeHeaders = Objects.isNull(responseConfig) || Objects.isNull(responseConfig.getIncludeHeaders())
                || Boolean.TRUE.equals(responseConfig.getIncludeHeaders());
        final String message = Objects.nonNull(responseConfig) && Objects.nonNull(responseConfig.getMessage())
                ? responseConfig.getMessage() : DEFAULT_MESSAGE;

        final Map<String, Object> context = new HashMap<>();
        context.put("type", type);
        context.put("keyStrategy", keyStrategy);
        context.put("keyHeader", keyHeader);
        context.put("capacity", capacity);
        context.put("refillTokens", refillTokens);
        context.put("refillDuration", refillDuration);
        context.put("hasOverdraft", hasOverdraft);
        context.put("statusCode", statusCode);
        context.put("includeHeaders", includeHeaders);
        context.put("message", message);
        context.put(TemplateContextConstants.IS_SPRING_BOOT_3, isSpringBoot3);

        if (hasOverdraft) {
            final RateLimitDefinition overdraft = global.getOverdraft();
            context.put("overdraftCapacity", Objects.nonNull(overdraft.getCapacity()) ? overdraft.getCapacity() : 20L);
            context.put("overdraftRefillTokens", Objects.nonNull(overdraft.getRefillTokens()) ? overdraft.getRefillTokens() : 20L);
            context.put("overdraftRefillDuration", Objects.nonNull(overdraft.getRefillDuration()) ? overdraft.getRefillDuration() : 10L);
        }

        this.generateRateLimiterService(outputDir, configPackage, configSubPackage, context);
        this.generateRateLimitingFilter(outputDir, configPackage, configSubPackage, context);

        if (RateLimitTypeEnum.REDIS.equals(type)) {
            this.generateRedisRateLimiterConfiguration(outputDir, configPackage, configSubPackage, context);
        }

        GeneratorContext.markGenerated(GeneratorConstants.GeneratorContextKeys.RATE_LIMITING_CONFIGURATION);
    }

    private void generateRateLimiterService(final String outputDir, final String configPackage,
            final String configSubPackage, final Map<String, Object> context) {

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, configPackage))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
                        "ratelimiting/rate-limiter-service.ftl", context));

        FileWriterUtils.writeToFile(outputDir, configSubPackage, "RateLimiterService.java", sb.toString());
    }

    private void generateRateLimitingFilter(final String outputDir, final String configPackage,
            final String configSubPackage, final Map<String, Object> context) {

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, configPackage))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
                        "ratelimiting/rate-limiting-filter.ftl", context));

        FileWriterUtils.writeToFile(outputDir, configSubPackage, "RateLimitingFilter.java", sb.toString());
    }

    private void generateRedisRateLimiterConfiguration(final String outputDir, final String configPackage,
            final String configSubPackage, final Map<String, Object> context) {

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(PACKAGE, configPackage))
                .append(FreeMarkerTemplateProcessorUtils.processTemplate(
                        "ratelimiting/redis-rate-limiter-configuration.ftl", context));

        FileWriterUtils.writeToFile(outputDir, configSubPackage, "RedisRateLimiterConfiguration.java", sb.toString());
    }

}
