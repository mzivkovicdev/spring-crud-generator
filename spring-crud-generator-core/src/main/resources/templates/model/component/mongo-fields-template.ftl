<#include "_common.ftl">
<#list fields as field>
    <#if field.id?has_content>
    @Id
    </#if><#t>
    <#if field.relation??>
    @DBRef
    </#if><#t>
    <#if field.relation?? && (field.relation.type == "OneToMany" || field.relation.type == "ManyToMany")>
    private ${relationCollectionType(field.relation)}<${field.resolvedType}> ${field.name} = new <#if relationCollectionType(field.relation) == "Set">HashSet<#else>ArrayList</#if><>();
    <#else>
    private ${field.resolvedType} ${field.name}<#if isCollection(field.resolvedType)> = new ${collectionImpl(field.resolvedType)}<>()</#if>;
    </#if>

</#list>
<#if !(embedded?? && embedded) && optimisticLocking>
    @Version
    private Long version;
</#if><#t>
<#if auditEnabled?? && auditEnabled>

    @CreatedDate
    private ${auditType} createdAt;

    @LastModifiedDate
    private ${auditType} updatedAt;
</#if><#t>
<#if softDeleteEnabled?? && softDeleteEnabled>

    private boolean deleted = Boolean.FALSE;
</#if><#t>
