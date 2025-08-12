<#list fields as field>
    <#if field.id>
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)</#if><#if field.type?lower_case == "enum">
    @Enumerated(EnumType.STRING)</#if><#if field.column??>
    @Column(
        <#assign first=true>
        <#if field.column.unique??><#if !first>, </#if>unique = ${field.column.unique?c}<#assign first=false></#if><#if field.column.nullable??><#if !first>, </#if>nullable = ${field.column.nullable?c}<#assign first=false></#if><#if field.column.insertable??><#if !first>, </#if>insertable = ${field.column.insertable?c}<#assign first=false></#if><#if field.column.updateable??><#if !first>, </#if>updatable = ${field.column.updateable?c}<#assign first=false></#if><#if field.column.length??><#if !first>, </#if>length = ${field.column.length?c}<#assign first=false></#if><#if field.column.precision??><#if !first>, </#if>precision = ${field.column.precision?c}</#if>
    )</#if>
    <#if field.relation??><#include "relation-field.ftl"></#if>
    <#if field.relation?? && (field.relation.type == "OneToMany" || field.relation.type == "ManyToMany")>
    private List<${field.resolvedType}> ${field.name};
    <#else>
    private ${field.resolvedType} ${field.name};
    </#if>
</#list>
<#if optimisticLocking>

    @Version
    private Integer version;
</#if>