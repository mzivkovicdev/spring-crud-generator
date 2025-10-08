package com.markozivkovic.codegen.constants;

public class JavaConstants {
    
    private JavaConstants(){

    }

    public static final String IMPORT = "import %s;\n";
    public static final String PACKAGE = "package %s;\n\n";

    public static final String JAVA_UTIL = "java.util";
    public static final String JAVA_UTIL_OBJECTS = JAVA_UTIL + ".Objects";
    public static final String JAVA_UTIL_OPTIONAL = JAVA_UTIL + ".Optional";
    public static final String JAVA_UTIL_UUID = JAVA_UTIL + ".UUID";
    public static final String JAVA_UTIL_LIST = JAVA_UTIL + ".List";
    public static final String JAVA_UTIL_STREAM = JAVA_UTIL + ".stream";
    public static final String JAVA_UTIL_STREAM_COLLECTORS = JAVA_UTIL_STREAM + ".Collectors";

    public static final String JAVA_MATH = "java.math";
    public static final String JAVA_MATH_BIG_DECIMAL = JAVA_MATH + ".BigDecimal";
    public static final String JAVA_MATH_BIG_INTEGER = JAVA_MATH + ".BigInteger";

    public static final String JAVA_TIME = "java.time";
    public static final String JAVA_TIME_LOCAL_DATE = JAVA_TIME + ".LocalDate";
    public static final String JAVA_TIME_LOCAL_DATE_TIME = JAVA_TIME + ".LocalDateTime";

}
