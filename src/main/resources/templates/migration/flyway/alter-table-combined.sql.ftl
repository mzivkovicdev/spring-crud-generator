<#include "_common.ftl">
<#assign isPostgres = (db?string == "POSTGRESQL")>
<#assign isMySql    = (db?string == "MYSQL")>
<#assign isMsSql    = (db?string == "MSSQL")>
<#if addedColumns?has_content>
<#list addedColumns as c>
ALTER TABLE ${quoteIdent(table)}
  ADD<#if !isMsSql> COLUMN</#if> ${quoteIdent(c.name)} ${c.type}
  <#if c.nullable?? && !c.nullable> NOT NULL</#if><#t>
  <#if c.defaultValue?? && c.defaultValue?has_content> DEFAULT ${c.defaultValue}</#if><#t>;
<#if c.unique?? && c.unique>
ALTER TABLE ${quoteIdent(table)}
  ADD CONSTRAINT uk_${table}_${c.name}
  UNIQUE (${quoteIdent(c.name)});
</#if><#t>
</#list>
</#if><#t>
<#if removedColumns?has_content>
<#list removedColumns as c>
ALTER TABLE ${quoteIdent(table)}
  DROP COLUMN ${quoteIdent(c)};
</#list>
</#if><#t>
<#if modifiedColumns?has_content>
<#list modifiedColumns as m>
<#if m.typeChanged?? && m.typeChanged>
<#if isPostgres>
ALTER TABLE ${quoteIdent(table)}
  ALTER COLUMN ${quoteIdent(m.name)} TYPE ${m.newType};
<#elseif isMySql>
ALTER TABLE ${quoteIdent(table)}
  MODIFY COLUMN ${quoteIdent(m.name)} ${m.newType}
  <#if m.newNullable?? && !m.newNullable> NOT NULL</#if>
  <#if m.newDefault?? && m.newDefault?has_content> DEFAULT ${m.newDefault}</#if>
;
</#if><#t>
</#if><#t>
<#if isPostgres && m.nullableChanged?? && m.nullableChanged>
ALTER TABLE ${quoteIdent(table)}
  ALTER COLUMN ${quoteIdent(m.name)}
  <#if m.newNullable?? && m.newNullable>DROP NOT NULL<#else>SET NOT NULL</#if>;
</#if>
<#if m.defaultChanged?? && m.defaultChanged>
ALTER TABLE ${quoteIdent(table)}
  ALTER COLUMN ${quoteIdent(m.name)}
  <#if m.newDefault?? && m.newDefault?has_content>
    SET DEFAULT ${m.newDefault}
  <#else>
    DROP DEFAULT
  </#if>;
</#if><#t>
<#if m.uniqueChanged?? && m.uniqueChanged>
ALTER TABLE ${quoteIdent(table)}
  DROP <#if isMySql>INDEX<#else>CONSTRAINT</#if> <#if isPostgres>IF EXISTS </#if>uk_${table}_${m.name};
<#if m.newUnique?? && m.newUnique>
ALTER TABLE ${quoteIdent(table)}
  ADD CONSTRAINT uk_${table}_${m.name}
  UNIQUE (${quoteIdent(m.name)});
</#if>
</#if><#t>
</#list>
</#if><#t>
<#if pkChanged?? && pkChanged && newPk?has_content>
<#if isMySql>
ALTER TABLE ${quoteIdent(table)}
  DROP PRIMARY KEY;
<#else>
ALTER TABLE ${quoteIdent(table)}
  DROP CONSTRAINT <#if isPostgres>IF EXISTS </#if>pk_${table};
</#if>

ALTER TABLE ${quoteIdent(table)}
  ADD CONSTRAINT pk_${table}
  PRIMARY KEY (
    <#list newPk as k>${quoteIdent(k)}<#if k_has_next>, </#if></#list>
  );
</#if><#t>
<#if removedFks?has_content>
<#list removedFks as fk>
<#if isMySql>
ALTER TABLE ${quoteIdent(table)}
  DROP FOREIGN KEY fk_${table}_${fk.column};
<#else>
ALTER TABLE ${quoteIdent(table)}
  DROP CONSTRAINT <#if isPostgres>IF EXISTS </#if>fk_${table}_${fk.column};
</#if>
</#list>
</#if><#t>
<#if addedFks?has_content>
<#list addedFks as fk>
ALTER TABLE ${quoteIdent(table)}
  ADD CONSTRAINT fk_${table}_${fk.column}
  FOREIGN KEY (${quoteIdent(fk.column)})
  REFERENCES ${quoteIdent(fk.refTable)} (${quoteIdent(fk.refColumn)});
</#list>
</#if><#t>
<#if auditAdded?? && auditAdded>
ALTER TABLE ${quoteIdent(table)} ADD<#if !isMsSql> COLUMN</#if> ${quoteIdent("created_at")} ${auditCreatedType} NOT NULL DEFAULT ${auditNowExpr};
ALTER TABLE ${quoteIdent(table)} ADD<#if !isMsSql> COLUMN</#if> ${quoteIdent("updated_at")} ${auditUpdatedType} NOT NULL DEFAULT ${auditNowExpr};
</#if><#t>
<#if auditRemoved?? && auditRemoved>
ALTER TABLE ${quoteIdent(table)} DROP COLUMN <#if isPostgres>IF EXISTS </#if>${quoteIdent("created_at")};
ALTER TABLE ${quoteIdent(table)} DROP COLUMN <#if isPostgres>IF EXISTS </#if>${quoteIdent("updated_at")};
</#if><#t>
<#if auditTypeChanged?? && auditTypeChanged>
ALTER TABLE ${quoteIdent(table)} ALTER COLUMN ${quoteIdent("created_at")} TYPE ${auditCreatedType};
ALTER TABLE ${quoteIdent(table)} ALTER COLUMN ${quoteIdent("updated_at")} TYPE ${auditUpdatedType};
</#if><#t>