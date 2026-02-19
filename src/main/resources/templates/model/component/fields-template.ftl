<#include "_common.ftl">
<#list fields as field>
    <#if field.id?has_content>
    @Id
    <#assign defaultSequence = (field.id.strategy == "AUTO" && (db == "MSSQL" || db == "POSTGRESQL" || db == "MYSQL" || db == "MARIADB")) />
    <#if field.id.strategy == "SEQUENCE" || defaultSequence>
    @SequenceGenerator(
        name = "${strippedModelName}_gen",
        sequenceName = "${field.id.generatorName! (storageName + "_id_seq")}",
        allocationSize = ${field.id.allocationSize!50},
        initialValue = ${field.id.initialValue!1}
    )
    <#elseif field.id.strategy == "TABLE">
    @TableGenerator(
        name = "${strippedModelName}_gen",
        table = "${field.id.generatorName! (storageName + "_id_gen")}",
        pkColumnName = "${field.id.pkColumnName! "gen_name"}",
        valueColumnName = "${field.id.valueColumnName! "gen_value"}",
        pkColumnValue = "${storageName}",
        allocationSize = ${field.id.allocationSize!50},
        initialValue = ${field.id.initialValue!1}
    )
    </#if><#t>
    @GeneratedValue(strategy = GenerationType.${field.id.strategy}<#if defaultSequence || field.id.strategy == "SEQUENCE" || field.id.strategy == "TABLE">, generator = "${strippedModelName}_gen"</#if>)
    </#if><#t>
    <#if field.type?lower_case == "enum">
    @Enumerated(EnumType.STRING)
    </#if><#t>
    <#if field.column??>
    @Column(
        <#assign first=true>
        <#if field.column.unique??><#if !first>, </#if>unique = ${field.column.unique?c}<#assign first=false></#if><#if field.column.nullable??><#if !first>, </#if>nullable = ${field.column.nullable?c}<#assign first=false></#if><#if field.column.insertable??><#if !first>, </#if>insertable = ${field.column.insertable?c}<#assign first=false></#if><#if field.column.updateable??><#if !first>, </#if>updatable = ${field.column.updateable?c}<#assign first=false></#if><#if field.column.length??><#if !first>, </#if>length = ${field.column.length?c}<#assign first=false></#if><#if field.column.precision??><#if !first>, </#if>precision = ${field.column.precision?c}</#if>
    )
    </#if>
    <#if field.relation??><#include "relation-field.ftl"></#if><#t>
    <#if field.relation?? && (field.relation.type == "OneToMany" || field.relation.type == "ManyToMany")><#t>
    private List<${field.resolvedType}> ${field.name};
    <#else>
    <#assign isJsonField = field.type?matches("^JSONB?<.+>$")>
    <#if isJsonField>
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    </#if><#t>
    <#if isBaseEntity && !isJsonField && isCollection(field.resolvedType)>
    @ElementCollection
    @CollectionTable(
        name = "${storageName}_${toSnakeCase(field.name)}",
        joinColumns = @JoinColumn(name = "${strippedModelName?uncap_first}_id")
    )
    <#if isBaseEntity && !isJsonField && isList(field.resolvedType)>
    @OrderColumn(name = "order_index")
    </#if><#t>
    </#if><#t>
    private ${field.resolvedType} ${field.name}<#if isCollection(field.resolvedType)> = new ${collectionImpl(field.resolvedType)}<>()</#if>;
    </#if>

</#list>
<#if !(embedded?? && embedded) && optimisticLocking>
    @Version
    private Integer version;
</#if><#t>
<#if auditEnabled?? && auditEnabled>

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private ${auditType} createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private ${auditType} updatedAt;
</#if><#t>