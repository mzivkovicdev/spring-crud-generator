public record ${className}TO(<#list inputArgs as field>${field}<#if field_has_next>, </#if></#list><#if auditEnabled?? && auditEnabled>, ${auditType} createdAt, ${auditType} updatedAt</#if>) {

}