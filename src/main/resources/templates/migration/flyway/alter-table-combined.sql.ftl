<#assign isPostgres = (db?string == "POSTGRESQL")>
<#assign isMySql    = (db?string == "MYSQL")>
<#if addedColumns?has_content>
<#list addedColumns as c>
ALTER TABLE ${table}
  ADD COLUMN ${c.name} ${c.type}
  <#if c.nullable?? && !c.nullable> NOT NULL</#if>
  <#if c.defaultValue?? && c.defaultValue?has_content> DEFAULT ${c.defaultValue}</#if>
;
<#if c.unique?? && c.unique>
ALTER TABLE ${table}
  ADD CONSTRAINT uk_${table}_${c.name}
  UNIQUE (${c.name});
</#if><#t>
</#list>
</#if><#t>
<#if removedColumns?has_content>
<#list removedColumns as c>
ALTER TABLE ${table}
  DROP COLUMN ${c};
</#list>
</#if><#t>
<#if modifiedColumns?has_content>
<#list modifiedColumns as m>
<#if m.typeChanged?? && m.typeChanged>
<#if isPostgres>
ALTER TABLE ${table}
  ALTER COLUMN ${m.name} TYPE ${m.newType};
<#elseif isMySql>
ALTER TABLE ${table}
  MODIFY COLUMN ${m.name} ${m.newType}
  <#if m.newNullable?? && !m.newNullable> NOT NULL</#if>
  <#if m.newDefault?? && m.newDefault?has_content> DEFAULT ${m.newDefault}</#if>
;
</#if><#t>
</#if><#t>
<#if isPostgres && m.nullableChanged?? && m.nullableChanged>
ALTER TABLE ${table}
  ALTER COLUMN ${m.name}
  <#if m.newNullable?? && m.newNullable>DROP NOT NULL<#else>SET NOT NULL</#if>;
</#if>
<#if m.defaultChanged?? && m.defaultChanged>
ALTER TABLE ${table}
  ALTER COLUMN ${m.name}
  <#if m.newDefault?? && m.newDefault?has_content>
    SET DEFAULT ${m.newDefault}
  <#else>
    DROP DEFAULT
  </#if>;
</#if><#t>
<#if m.uniqueChanged?? && m.uniqueChanged>
ALTER TABLE ${table}
  DROP CONSTRAINT IF EXISTS uk_${table}_${m.name};
<#if m.newUnique?? && m.newUnique>
ALTER TABLE ${table}
  ADD CONSTRAINT uk_${table}_${m.name}
  UNIQUE (${m.name});
</#if>
</#if><#t>
</#list>
</#if><#t>
<#if pkChanged?? && pkChanged && newPk?has_content>
ALTER TABLE ${table}
  DROP CONSTRAINT IF EXISTS pk_${table};

ALTER TABLE ${table}
  ADD CONSTRAINT pk_${table}
  PRIMARY KEY (
    <#list newPk as k>${k}<#if k_has_next>, </#if></#list>
  );
</#if><#t>
<#if removedFks?has_content>
<#list removedFks as fk>
ALTER TABLE ${table}
  DROP CONSTRAINT IF EXISTS fk_${table}_${fk.column};
</#list>
</#if><#t>
<#if addedFks?has_content>
<#list addedFks as fk>
ALTER TABLE ${table}
  ADD CONSTRAINT fk_${table}_${fk.column}
  FOREIGN KEY (${fk.column})
  REFERENCES ${fk.refTable} (${fk.refColumn});
</#list>
</#if><#t>
<#if auditAdded?? && auditAdded>
ALTER TABLE ${table} ADD COLUMN created_at ${auditCreatedType} NOT NULL DEFAULT ${auditNowExpr};
ALTER TABLE ${table} ADD COLUMN updated_at ${auditUpdatedType} NOT NULL DEFAULT ${auditNowExpr};
</#if><#t>
<#if auditRemoved?? && auditRemoved>
ALTER TABLE ${table} DROP COLUMN IF EXISTS created_at;
ALTER TABLE ${table} DROP COLUMN IF EXISTS updated_at;
</#if><#t>
<#if auditTypeChanged?? && auditTypeChanged>
ALTER TABLE ${table} ALTER COLUMN created_at TYPE ${auditCreatedType};
ALTER TABLE ${table} ALTER COLUMN updated_at TYPE ${auditUpdatedType};
</#if><#t>