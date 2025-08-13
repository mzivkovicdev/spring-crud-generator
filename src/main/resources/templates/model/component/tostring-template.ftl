    @Override
    public String toString() {
        return "${className}{" +
        <#list fields as field>
            <#assign getterPrefix = (field.type == "boolean" || field.type == "Boolean")?then("is", "get")>
            <#assign getter = getterPrefix + field.name?cap_first>
            <#if field?index == 0>
            " ${field.name}='" + ${getter}() + "'" +
            <#else>
            ", ${field.name}='" + ${getter}() + "'" +
            </#if>
        </#list>
        "}";
    }
