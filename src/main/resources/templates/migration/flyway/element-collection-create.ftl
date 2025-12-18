<#-- ElementCollection tables -->
<#if collectionTables?has_content>
<#list collectionTables as ct>
CREATE TABLE IF NOT EXISTS ${ct.tableName} (
  ${ct.joinColumn} ${ct.ownerPkSqlType} NOT NULL,
  ${ct.valueColumn} ${ct.valueSqlType} NOT NULL
  <#if ct.isList>, ${ct.orderColumn} INTEGER NOT NULL</#if>,
  CONSTRAINT fk_${ct.tableName}_${ct.joinColumn}
    FOREIGN KEY (${ct.joinColumn})
    REFERENCES ${ct.ownerTable} (${ct.ownerPkColumn})
  <#if !ct.isList && (ct.needsUnique?? && ct.needsUnique)>
  , CONSTRAINT uk_${ct.tableName}_${ct.valueColumn}
    UNIQUE (${ct.joinColumn}, ${ct.valueColumn})
  </#if>
);
</#list>
</#if>