<#list fields as field>
    <#if field.relation?? && (field.relation.type == "OneToMany" || field.relation.type == "ManyToMany")>
    public List<${field.resolvedType}> get${field.name?cap_first}() {
        return this.${field.name};
    }

    public ${className} set${field.name?cap_first}(final List<${field.resolvedType}> ${field.name}) {
        this.${field.name} = ${field.name};
        return this;
    }
    <#else>
    public ${field.resolvedType} get${field.name?cap_first}() {
        return this.${field.name};
    }

    public ${className} set${field.name?cap_first}(final ${field.resolvedType} ${field.name}) {
        this.${field.name} = ${field.name};
        return this;
    }
    </#if>

</#list>
<#if auditEnabled?? && auditEnabled>
    public ${auditType} getCreatedAt() {
        return this.createdAt;
    }

    public ${auditType} getUpdatedAt() {
        return this.updatedAt;
    }
</#if>