package com.markozivkovic.codegen.constants;

public class JPAConstants {
    
    private JPAConstants(){

    }

    public static final String ID_ANNOTATION = "@Id";
    public static final String GENERATED_VALUE_ANNOTATION = "@GeneratedValue(strategy = GenerationType.IDENTITY)";
    public static final String ENTITY_ANNOTATION = "@Entity";
    public static final String TABLE_ANNOTATION = "@Table(name = \"%s\")";

    public static final String JAKARTA_PERSISTENCE_PACKAGE = "jakarta.persistence";
    public static final String JAKARTA_PERSISTANCE_ENTITY = JAKARTA_PERSISTENCE_PACKAGE + ".Entity";
    public static final String JAKARTA_PERSISTANCE_TABLE = JAKARTA_PERSISTENCE_PACKAGE + ".Table";
    public static final String JAKARTA_PERSISTANCE_ID = JAKARTA_PERSISTENCE_PACKAGE + ".Id";
    public static final String JAKARTA_PERSISTANCE_GENERATED_VALUE = JAKARTA_PERSISTENCE_PACKAGE + ".GeneratedValue";
    public static final String JAKARTA_PERSISTANCE_GENERATION_TYPE = JAKARTA_PERSISTENCE_PACKAGE + ".GenerationType";

    public static final String SPRING_DATA_PACKAGE = "org.springframework.data";
    public static final String SPRING_DATA_PACKAGE_JPA_REPOSITORY = SPRING_DATA_PACKAGE + ".jpa.repository.JpaRepository";
    public static final String SPRING_DATA_PACKAGE_DOMAIN = SPRING_DATA_PACKAGE + ".domain";
    public static final String SPRING_DATA_PACKAGE_DOMAIN_PAGE = SPRING_DATA_PACKAGE_DOMAIN + ".Page";

}
