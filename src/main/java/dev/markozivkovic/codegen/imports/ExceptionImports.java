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

package dev.markozivkovic.codegen.imports;

import static dev.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import dev.markozivkovic.codegen.models.PackageConfiguration;
import dev.markozivkovic.codegen.utils.PackageUtils;

public class ExceptionImports {

    private static final String INVALID_RESOURCE_STATE_EXCEPTION = "InvalidResourceStateException";
    private static final String RESOURCE_NOT_FOUND_EXCEPTION = "ResourceNotFoundException";

    private static final String HTTP_RESPONSE = "HttpResponse";
    
    private ExceptionImports() {}

    /**
     * Computes the necessary imports for the global rest exception handler, given the relations configuration.
     * 
     * @param hasRelations         whether the project has any relations
     * @param outputDir            the directory where the generated code will be written
     * @param packageConfiguration the package configuration
     * @return A string containing the necessary import statements for the global rest exception handler.
     */
    public static String computeGlobalRestExceptionHandlerProjectImports(final boolean hasRelations, final String outputDir,
                final PackageConfiguration packageConfiguration) {

        return computeGlobalExceptionHandlerProjectImports(hasRelations, outputDir, true, packageConfiguration);
    }

    /**
     * Computes the necessary imports for the global graphql exception handler, given the relations configuration.
     * 
     * @param hasRelations         whether the project has any relations
     * @param outputDir            the directory where the generated code will be written
     * @param packageConfiguration the package configuration
     * @return A string containing the necessary import statements for the global graphql exception handler.
     */
    public static String computeGlobalGraphQlExceptionHandlerProjectImports(final boolean hasRelations, final String outputDir,
            final PackageConfiguration packageConfiguration) {

        return computeGlobalExceptionHandlerProjectImports(hasRelations, outputDir, false, packageConfiguration);
    }

    /**
     * Computes the necessary imports for the global exception handler, given the relations configuration.
     *
     * @param hasRelations         whether the project has any relations
     * @param outputDir            the directory where the generated code will be written
     * @param importHttpResponse   whether to include the HttpResponse import
     * @param packageConfiguration the package configuration
     * @return A string containing the necessary import statements for the global exception handler.
     */
    private static String computeGlobalExceptionHandlerProjectImports(final boolean hasRelations, final String outputDir,
                final boolean importHttpResponse, final PackageConfiguration packageConfiguration) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeExceptionPackage(packagePath, packageConfiguration), RESOURCE_NOT_FOUND_EXCEPTION)));
        if (importHttpResponse) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeExceptionResponsePackage(packagePath, packageConfiguration), HTTP_RESPONSE)));
        }

        if (hasRelations) {
            imports.add(String.format(IMPORT, PackageUtils.join(PackageUtils.computeExceptionPackage(packagePath, packageConfiguration), INVALID_RESOURCE_STATE_EXCEPTION)));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

}
