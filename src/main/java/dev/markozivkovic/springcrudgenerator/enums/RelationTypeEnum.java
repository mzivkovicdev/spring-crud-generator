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

package dev.markozivkovic.springcrudgenerator.enums;

import java.util.List;

public enum RelationTypeEnum {
    
    ONE_TO_ONE("OneToOne"),
    ONE_TO_MANY("OneToMany"),
    MANY_TO_ONE("ManyToOne"),
    MANY_TO_MANY("ManyToMany");

    private final String key;

    RelationTypeEnum(final String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public static List<String> getDefaultLazyTypes() {
        return List.of(ONE_TO_MANY.getKey(), MANY_TO_MANY.getKey());
    }

    public static List<String> getDefaultEagerTypes() {
        return List.of(ONE_TO_ONE.getKey(), MANY_TO_ONE.getKey());
    }

}
