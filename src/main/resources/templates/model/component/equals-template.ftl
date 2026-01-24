    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ${className})) {
            return false;
        }
        final ${className} other = (${className}) o;
<#if embedded?? && embedded>
        return <#list fieldNames as field><#if field?index == 0>Objects.equals(${field}, other.${field})<#else>
                && Objects.equals(${field}, other.${field})</#if></#list>;
<#else>
        return Objects.nonNull(${idField}) && ${idField}.equals(other.get${idField?cap_first}());
</#if><#t>
    }
    