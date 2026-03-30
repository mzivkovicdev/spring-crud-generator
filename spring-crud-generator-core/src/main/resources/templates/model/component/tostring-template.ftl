    @Override
    public String toString() {
        return "${className}{" +
        <#list fields as field>
            <#assign getter = "get" + field.name?cap_first>
            <#if field?index == 0>
            " ${field.name}='" + ${getter}() + "'" +
            <#else>
            ", ${field.name}='" + ${getter}() + "'" +
            </#if>
        </#list>
        "}";
    }
