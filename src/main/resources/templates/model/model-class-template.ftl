<#if tableName??>
@Entity
@Table(name = "${tableName}")
<#if auditEnabled?? && auditEnabled>
@EntityListeners(AuditingEntityListener.class)
</#if><#t>
<#if !openInView && lazyFields?? && lazyFields?has_content>
@NamedEntityGraph(
    name = "${entityGraphName}",
    attributeNodes = {
        <#list lazyFields as lazyField>
        @NamedAttributeNode("${lazyField}")<#if lazyField_has_next>,</#if>
        </#list>
    }
)
</#if><#t>
</#if><#t>
<#if softDeleteEnabled?? && softDeleteEnabled>
@SQLDelete(sql = "UPDATE ${tableName} SET deleted = <#if db == "MSSQL">1<#else>true</#if> WHERE ${idField} = ?<#if optimisticLocking?? && optimisticLocking> AND version = ?</#if>")
@SQLRestriction("deleted = false")
</#if><#t>
public class ${className} {

${fields}
${defaultConstructor}
${constructor}
${gettersAndSetters}
${hashCode}
${equals}
${toString}
}