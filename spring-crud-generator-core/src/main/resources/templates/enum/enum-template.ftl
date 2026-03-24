public enum ${enumName} {
    
    <#list values as val>
    ${val?upper_case}("${val?upper_case}")<#if val_has_next>, 
</#if></#list>;

    private final String value;

    ${enumName}(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
