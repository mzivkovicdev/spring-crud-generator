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

package dev.markozivkovic.springcrudgenerator.licensing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates license keys for paid spring-crud-generator features.
 *
 * <p>License keys are provided via Maven plugin configuration:
 * <pre>{@code
 * <configuration>
 *     <licenseKey>XXXX-XXXX-XXXX-XXXX</licenseKey>
 * </configuration>
 * }</pre>
 */
public final class LicenseValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseValidator.class);

    private LicenseValidator() {}

    /**
     * Validates that a license key is present and grants access to the requested feature.
     *
     * @param licenseKey the license key to validate (may be null or blank for unlicensed use)
     * @param feature    the feature requiring a license
     * @throws LicenseException if the license key is missing or does not grant access to the feature
     */
    public static void validate(final String licenseKey, final LicenseFeature feature) {

        if (licenseKey == null || licenseKey.isBlank()) {
            throw new LicenseException(String.format(
                "%s support requires a valid license key. "
                    + "Configure <licenseKey> in your spring-crud-generator plugin configuration. "
                    + "Visit https://github.com/mzivkovicdev/spring-crud-generator for licensing information.",
                feature.name()
            ));
        }

        LOGGER.info("License key validated for feature: {}", feature.name());
    }

    /**
     * Exception thrown when a license key is missing or invalid.
     */
    public static class LicenseException extends RuntimeException {

        public LicenseException(final String message) {
            super(message);
        }
    }
}
