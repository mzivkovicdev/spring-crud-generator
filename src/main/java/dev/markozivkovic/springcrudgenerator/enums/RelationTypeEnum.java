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
