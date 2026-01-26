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

package dev.markozivkovic.springcrudgenerator.constants;

/**
 * Keys for additional configuration options used in the code generation process.
 */
public class AdditionalConfigurationConstants {
    
    private AdditionalConfigurationConstants() {}

    public static final String REST_BASEPATH = "rest.basepath";

    public static final String OPT_LOCK_RETRY_CONFIGURATION = "optimisticLocking.retry.config";
    public static final String OPT_LOCK_MAX_ATTEMPTS = "optimisticLocking.retry.maxAttempts";
    public static final String OPT_LOCK_BACKOFF_DELAY_MS = "optimisticLocking.backoff.delayMs";
    public static final String OPT_LOCK_BACKOFF_MAX_DELAY_MS = "optimisticLocking.backoff.maxDelayMs";
    public static final String OPT_LOCK_BACKOFF_MULTIPLIER = "optimisticLocking.backoff.multiplier";

}
