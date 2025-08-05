<#list fields as field>
    <#if field.id>
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)</#if><#if field.type?lower_case == "enum">
    @Enumerated(EnumType.STRING)</#if>
    <#if field.relation??><#include "relation-field.ftl"></#if>
    <#if field.relation?? && (field.relation.type == "OneToMany" || field.relation.type == "ManyToMany")>
    private List<${field.resolvedType}> ${field.name};
    <#else>
    private ${field.resolvedType} ${field.name};
    </#if>
</#list>
