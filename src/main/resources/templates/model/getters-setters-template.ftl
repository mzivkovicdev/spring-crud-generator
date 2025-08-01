<#list fields as field>
    public ${field.resolvedType} get${field.name?cap_first}() {
        return this.${field.name};
    }

    public ${className} set${field.name?cap_first}(final ${field.resolvedType} ${field.name}) {
        this.${field.name} = ${field.name};
        return this;
    }

</#list>