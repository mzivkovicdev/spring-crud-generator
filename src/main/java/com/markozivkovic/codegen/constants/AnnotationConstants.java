package com.markozivkovic.codegen.constants;

public class AnnotationConstants {
    
    private AnnotationConstants(){

    }

    public static final String ID_ANNOTATION = "@Id";
    public static final String GENERATED_VALUE_ANNOTATION = "@GeneratedValue(strategy = GenerationType.IDENTITY)";
    public static final String ENTITY_ANNOTATION = "@Entity";
    public static final String TABLE_ANNOTATION = "@Table(name = \"%s\")";
    public static final String ENTITY_LISTENERS_ANNOTATION = "@EntityListeners({%s})";
    public static final String AUDITING_ENTITY_LISTENER_CLASS = "AuditingEntityListener.class";
    public static final String TRANSACTIONAL_ANNOTATION = "@Transactional";
}
