package com.markozivkovic.codegen.constants;

public class JavaConstants {
    
    private JavaConstants(){

    }

    public static final String IMPORT = "import %s;\n";
    public static final String PACKAGE = "package %s;\n\n";

    public static final String JAVA_UTIL = "java.util";
    public static final String JAVA_UTIL_OBJECTS = JAVA_UTIL + ".Objects";
    public static final String JAVA_UTIL_UUID = JAVA_UTIL + ".UUID";

    public static final String JAVA_TIME = "java.time";
    public static final String JAVA_TIME_LOCAL_DATE = JAVA_TIME + ".LocalDate";
    public static final String JAVA_TIME_LOCAL_DATE_TIME = JAVA_TIME + ".LocalDateTime";

}
