    public ${className}(<#list inputArgs as field>${field}<#if field_has_next>, </#if></#list>) {
        <#list nonIdFieldNames as field>
        this.${field} = ${field};
        </#list>
    }
