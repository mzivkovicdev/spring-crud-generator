package com.markozivkovic.codegen.imports;

import static com.markozivkovic.codegen.constants.ImportConstants.IMPORT;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.markozivkovic.codegen.constants.GeneratorConstants.DefaultPackageLayout;
import com.markozivkovic.codegen.utils.PackageUtils;

public class ExceptionImports {

    private static final String INVALID_RESOURCE_STATE_EXCEPTION = "InvalidResourceStateException";
    private static final String RESOURCE_NOT_FOUND_EXCEPTION = "ResourceNotFoundException";

    private static final String HTTP_RESPONSE = "HttpResponse";
    
    private ExceptionImports() {}

    /**
     * Computes the necessary imports for the global rest exception handler, given the relations configuration.
     * 
     * @param hasRelations whether the project has any relations
     * @param outputDir the directory where the generated code will be written
     * @return A string containing the necessary import statements for the global rest exception handler.
     */
    public static String computeGlobalRestExceptionHandlerProjectImports(final boolean hasRelations, final String outputDir) {

        return computeGlobalExceptionHandlerProjectImports(hasRelations, outputDir, true);
    }

    /**
     * Computes the necessary imports for the global graphql exception handler, given the relations configuration.
     * 
     * @param hasRelations whether the project has any relations
     * @param outputDir the directory where the generated code will be written
     * @return A string containing the necessary import statements for the global graphql exception handler.
     */
    public static String computeGlobalGraphQlExceptionHandlerProjectImports(final boolean hasRelations, final String outputDir) {

        return computeGlobalExceptionHandlerProjectImports(hasRelations, outputDir, false);
    }

    /**
     * Computes the necessary imports for the global exception handler, given the relations configuration.
     *
     * @param hasRelations       whether the project has any relations
     * @param outputDir          the directory where the generated code will be written
     * @param importHttpResponse whether to include the HttpResponse import
     * @return A string containing the necessary import statements for the global exception handler.
     */
    private static String computeGlobalExceptionHandlerProjectImports(final boolean hasRelations, final String outputDir, final boolean importHttpResponse) {

        final Set<String> imports = new LinkedHashSet<>();

        final String packagePath = PackageUtils.getPackagePathFromOutputDir(outputDir);
        imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.EXCEPTIONS, RESOURCE_NOT_FOUND_EXCEPTION)));
        if (importHttpResponse) {
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.EXCEPTIONS, DefaultPackageLayout.RESPONSES, HTTP_RESPONSE)));
        }

        if (hasRelations) {
            imports.add(String.format(IMPORT, PackageUtils.join(packagePath, DefaultPackageLayout.EXCEPTIONS, INVALID_RESOURCE_STATE_EXCEPTION)));
        }

        return imports.stream()
                .sorted()
                .collect(Collectors.joining());
    }

}
