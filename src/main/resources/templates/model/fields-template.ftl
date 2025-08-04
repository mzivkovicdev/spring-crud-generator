<#list fields as field>
    <#if field.id>
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)</#if><#if field.type?lower_case == "enum">
    @Enumerated(EnumType.STRING)</#if>
    <#if field.relation??><#include "relation-field.ftl"></#if>
    private ${field.resolvedType} ${field.name};
</#list>
