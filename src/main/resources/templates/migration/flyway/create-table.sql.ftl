<#include "_common.ftl">
<#assign hasChecks = (checks?? && (checks?size > 0))>
<#assign uniqueCols = []>
<#list columns as c>
  <#if c.unique>
    <#assign uniqueCols = uniqueCols + [c]>
  </#if>
</#list>
<#assign hasUniques = (uniqueCols?size > 0)>
CREATE TABLE<#if db != "MSSQL"> IF NOT EXISTS</#if> ${quoteIdent(tableName)} (
<#list columns as c>
  ${quoteIdent(c.name)} ${c.sqlType}<#if c.defaultExpr??> DEFAULT ${c.defaultExpr}</#if><#if !c.nullable> NOT NULL</#if><#if (c?has_next) || (auditEnabled) || (pkColumns??) || hasChecks || hasUniques>,</#if>
</#list>
<#if auditEnabled>
  created_at ${auditCreatedType} NOT NULL DEFAULT ${auditNowExpr},
  updated_at ${auditUpdatedType} NOT NULL DEFAULT ${auditNowExpr}<#if (pkColumns??) || hasChecks || hasUniques>,</#if>
</#if>
<#if pkColumns??>
  CONSTRAINT pk_${tableName} PRIMARY KEY (${pkColumns})<#if hasChecks || hasUniques>,</#if>
</#if>
<#if hasChecks>
  <#list checks as chk>
  CONSTRAINT ${chk.name} CHECK (${chk.expr})<#if chk?has_next || hasUniques>,</#if>
  </#list>
</#if>
<#if hasUniques>
  <#list uniqueCols as u>
  CONSTRAINT uk_${tableName}_${u.name} UNIQUE (${quoteIdent(u.name)})<#if u?has_next>,</#if>
  </#list>
</#if>
);