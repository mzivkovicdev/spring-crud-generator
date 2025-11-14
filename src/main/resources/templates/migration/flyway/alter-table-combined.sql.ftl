<#if addedColumns?has_content>
<#list addedColumns as c>
ALTER TABLE ${table} ADD COLUMN ${c.name} ${c.type};
</#list>
</#if>

<#-- Removed columns -->
<#if removedColumns?has_content>
<#list removedColumns as c>
ALTER TABLE ${table} DROP COLUMN ${c};
</#list>
</#if>

<#-- Modified columns -->
<#if modifiedColumns?has_content>
<#list modifiedColumns as m>

<#if m.typeChanged?? && m.typeChanged>
ALTER TABLE ${table} ALTER COLUMN ${m.name} TYPE ${m.newType};
</#if>

<#if m.nullableChanged?? && m.nullableChanged>
ALTER TABLE ${table} ALTER COLUMN ${m.name} <#if m.newNullable>DROP NOT NULL<#else>SET NOT NULL</#if>;
</#if>

<#if m.uniqueChanged?? && m.uniqueChanged>
-- NOTE: handle unique via constraint (add/drop). Needs constraint name convention.
</#if>

<#if m.defaultChanged?? && m.defaultChanged>
ALTER TABLE ${table} ALTER COLUMN ${m.name}
  <#if m.newDefault?? && m.newDefault?has_content>SET DEFAULT ${m.newDefault}<#else>DROP DEFAULT</#if>;
</#if>
</#list>
</#if>

<#if pkChanged?? && pkChanged>
ALTER TABLE ${table} DROP CONSTRAINT IF EXISTS ${table}_pkey;
ALTER TABLE ${table} ADD PRIMARY KEY (<#list newPk as k>${k}<#if k_has_next>, </#if></#list>);
</#if>

<#if removedFks?has_content>
<#list removedFks as fk>
-- FK REMOVE: ${fk}
-- ALTER TABLE ${table} DROP CONSTRAINT <constraint_name>;
</#list>
</#if>

<#if addedFks?has_content>
<#list addedFks as fk>
-- FK ADD: ${fk}
-- ALTER TABLE ${table} ADD CONSTRAINT <constraint_name> FOREIGN KEY (...) REFERENCES ... (...);
</#list>
</#if>