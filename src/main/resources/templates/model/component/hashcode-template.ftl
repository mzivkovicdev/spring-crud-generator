    @Override
    public int hashCode() {
<#if embedded?? && embedded>
        return Objects.hash(<#list fieldNames as field><#if field?has_next>${field}, <#else>${field}</#if></#list>);
<#else>
        return getClass().hashCode();
</#if><#t>
    }
    