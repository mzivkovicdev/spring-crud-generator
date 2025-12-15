<#assign hasChecks = (checks?? && (checks?size > 0))>

CREATE TABLE IF NOT EXISTS ${tableName} (
<#list columns as c>
  ${c.name} ${c.sqlType}<#if c.defaultExpr??> DEFAULT ${c.defaultExpr}</#if><#if !c.nullable> NOT NULL</#if><#if c.unique> UNIQUE</#if><#if (c?has_next) || (auditEnabled) || (pkColumns??) || hasChecks>,</#if>
</#list>
<#if auditEnabled>
  created_at ${auditCreatedType} NOT NULL DEFAULT ${auditNowExpr},
  updated_at ${auditUpdatedType} NOT NULL DEFAULT ${auditNowExpr}<#if (pkColumns??) || hasChecks>,</#if>
</#if>
<#if pkColumns??>
  CONSTRAINT pk_${tableName} PRIMARY KEY (${pkColumns})<#if hasChecks> ,</#if>
</#if>
<#if hasChecks>
  <#list checks as chk>CONSTRAINT ${chk.name} CHECK (${chk.expr})<#if chk?has_next>,</#if></#list>
</#if>
);
