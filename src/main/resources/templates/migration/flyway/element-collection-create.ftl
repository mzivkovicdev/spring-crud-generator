<#include "_common.ftl">
<#if collectionTables?has_content>
<#list collectionTables as ct>
CREATE TABLE<#if db != "MSSQL"> IF NOT EXISTS</#if> ${quoteIdent(ct.tableName)} (
  ${quoteIdent(ct.joinColumn)} ${ct.ownerPkSqlType} NOT NULL,
  ${quoteIdent(ct.valueColumn)} ${ct.valueSqlType} NOT NULL
  <#if ct.isList>, ${quoteIdent(ct.orderColumn)} INTEGER NOT NULL</#if>,
  CONSTRAINT fk_${ct.tableName}_${ct.joinColumn}
    FOREIGN KEY (${quoteIdent(ct.joinColumn)})
    REFERENCES ${quoteIdent(ct.ownerTable)} (${quoteIdent(ct.ownerPkColumn)})
  <#if !ct.isList && (ct.needsUnique?? && ct.needsUnique)>
  , CONSTRAINT uk_${ct.tableName}_${ct.valueColumn}
    UNIQUE (${quoteIdent(ct.joinColumn)}, ${quoteIdent(ct.valueColumn)})
  </#if>
);
</#list>
</#if>