package dev.markozivkovic.springcrudgenerator.enums;

public enum FetchTypeEnum {
    
    EAGER("EAGER"),
    LAZY("LAZY");

    private final String key;

    FetchTypeEnum(final String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

}
