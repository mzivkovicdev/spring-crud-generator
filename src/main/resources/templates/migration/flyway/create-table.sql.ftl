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
  ${quoteIdent("created_at")} ${auditCreatedType} NOT NULL DEFAULT ${auditNowExpr},
  ${quoteIdent("updated_at")} ${auditUpdatedType} NOT NULL DEFAULT ${auditNowExpr}<#if (pkColumns??) || hasChecks || hasUniques>,</#if>
</#if>
<#if softDeleteEnabled?? && softDeleteEnabled>
  ${quoteIdent("deleted")}<#if db == "MSSQL"> bit DEFAULT 0 NOT NULL<#else> BOOLEAN DEFAULT FALSE NOT NULL</#if><#if (pkColumns??) || hasChecks || hasUniques>,</#if>
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
<#if softDeleteEnabled?? && softDeleteEnabled>
<#if db == "MSSQL">
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_${tableName}_deleted' AND object_id = OBJECT_ID('${tableName}'))
CREATE INDEX ix_${tableName}_deleted ON ${quoteIdent(tableName)} (${quoteIdent("deleted")});
<#else>
CREATE INDEX IF NOT EXISTS ix_${tableName}_deleted ON ${quoteIdent(tableName)} (${quoteIdent("deleted")});
</#if>
</#if>