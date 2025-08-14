    @Override
    public int hashCode() {
        return Objects.hash(<#list fieldNames as field><#if field?has_next>${field}, <#else>${field}</#if></#list>);
    }
    