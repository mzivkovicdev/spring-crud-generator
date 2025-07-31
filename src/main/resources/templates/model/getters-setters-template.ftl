<#list fields as field>
    public ${field.type} get${field.name?cap_first}() {
        return this.${field.name};
    }

    public ${className} set${field.name?cap_first}(final ${field.type} ${field.name}) {
        this.${field.name} = ${field.name};
        return this;
    }

</#list>