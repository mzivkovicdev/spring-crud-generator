package com.markozivkovic.codegen.constants;

public class JPAConstants {
    
    private JPAConstants(){

    }

    public static final String ID_ANNOTATION = "@Id";
    public static final String GENERATED_VALUE_ANNOTATION = "@GeneratedValue(strategy = GenerationType.IDENTITY)";
    public static final String ENTITY_ANNOTATION = "@Entity";
    public static final String TABLE_ANNOTATION = "@Table(name = \"%s\")";
    public static final String ENTITY_LISTENERS_ANNOTATION = "@EntityListeners({%s})";
    public static final String AUDITING_ENTITY_LISTENER_CLASS = "AuditingEntityListener.class";

    public static final String SPRING_DATA_PACKAGE = "org.springframework.data";
    public static final String SPRING_DATA_PACKAGE_JPA_REPOSITORY = SPRING_DATA_PACKAGE + ".jpa.repository.JpaRepository";
    public static final String SPRING_DATA_PACKAGE_DOMAIN = SPRING_DATA_PACKAGE + ".domain";
    public static final String SPRING_DATA_PACKAGE_DOMAIN_PAGE = SPRING_DATA_PACKAGE_DOMAIN + ".Page";
    public static final String SPRING_DATA_PACKAGE_DOMAIN_PAGE_IMPL = SPRING_DATA_PACKAGE_DOMAIN + ".PageImpl";
    public static final String SPRING_DATA_PACKAGE_DOMAIN_PAGE_REQUEST = SPRING_DATA_PACKAGE_DOMAIN + ".PageRequest";
    
    public static final String SPRING_DATA_JPA_DOMAIN_SUPPORT_AUDITING_ENTITY_LISTENER =
            SPRING_DATA_PACKAGE + ".jpa.domain.support.AuditingEntityListener";

    public static final String SPRING_DATA_ANNOTATION_CREATED_DATE = SPRING_DATA_PACKAGE + ".annotation.CreatedDate";
    public static final String SPRING_DATA_ANNOTATION_LAST_MODIFIED_DATE = SPRING_DATA_PACKAGE + ".annotation.LastModifiedDate";

    public static final String ORG_HIBERNATE = "org.hibernate";
    public static final String ORG_HIBERNATE_ANNOTATIONS = ORG_HIBERNATE + ".annotations";
    public static final String ORG_HIBERNATE_ANNOTATIONS_JDBC_TYPE_CODE = ORG_HIBERNATE_ANNOTATIONS + ".JdbcTypeCode";
    public static final String ORG_HIBERNATE_TYPE = ORG_HIBERNATE + ".type";
    public static final String ORG_HIBERNATE_TYPE_SQL_TYPES = ORG_HIBERNATE_TYPE + ".SqlTypes";

}
