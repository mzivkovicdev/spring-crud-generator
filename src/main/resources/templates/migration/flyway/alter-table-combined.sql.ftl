<#include "_common.ftl">
<#assign isPostgres = (db?string == "POSTGRESQL")>
<#assign isMySql    = (db?string == "MYSQL")>
<#assign isMariaDB  = (db?string == "MARIADB")>
<#assign isMsSql    = (db?string == "MSSQL")>
<#assign softDeleteOn  = (softDeleteEnabled?? && softDeleteEnabled)>
<#assign softDeleteOff = (softDeleteEnabled?? && !softDeleteEnabled)>
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
<#if softDeleteOn>
ALTER TABLE ${quoteIdent(table)}
  ADD<#if !isMsSql> COLUMN</#if> ${quoteIdent("deleted")}<#if isMsSql> bit DEFAULT 0 NOT NULL<#else> BOOLEAN DEFAULT FALSE NOT NULL</#if>;
<#if isMsSql>
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_${table}_deleted' AND object_id = OBJECT_ID('${table}'))
CREATE INDEX ix_${table}_deleted ON ${quoteIdent(table)} (${quoteIdent("deleted")});
<#else>
CREATE INDEX ix_${table}_deleted ON ${quoteIdent(table)} (${quoteIdent("deleted")});
</#if>
</#if><#t>
<#if removedColumns?has_content>
<#list removedColumns as c>
ALTER TABLE ${quoteIdent(table)}
  DROP COLUMN ${quoteIdent(c)};
</#list>
</#if><#t>
<#if softDeleteOff>
<#if isPostgres>
DROP INDEX IF EXISTS ix_${table}_deleted;
<#elseif isMySql || isMariaDB>
DROP INDEX ix_${table}_deleted ON ${quoteIdent(table)};
<#elseif isMsSql>
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_${table}_deleted' AND object_id = OBJECT_ID('${table}'))
DROP INDEX ix_${table}_deleted ON ${quoteIdent(table)};
</#if>
ALTER TABLE ${quoteIdent(table)} DROP COLUMN <#if isPostgres>IF EXISTS </#if>${quoteIdent("deleted")};
</#if><#t>
<#if modifiedColumns?has_content>
<#list modifiedColumns as m>
<#if m.typeChanged?? && m.typeChanged>
<#if isPostgres>
ALTER TABLE ${quoteIdent(table)}
  ALTER COLUMN ${quoteIdent(m.name)} TYPE ${m.newType};
<#elseif isMySql || isMariaDB>
ALTER TABLE ${quoteIdent(table)}
  MODIFY COLUMN ${quoteIdent(m.name)} ${m.newType}
  <#if m.newNullable?? && !m.newNullable> NOT NULL</#if>
  <#if m.newDefault?? && m.newDefault?has_content> DEFAULT ${m.newDefault}</#if>
;
<#elseif isMsSql>
ALTER TABLE ${quoteIdent(table)}
  ALTER COLUMN ${quoteIdent(m.name)} ${m.newType}<#if m.newNullable??><#if m.newNullable> NULL<#else> NOT NULL</#if></#if>;
</#if><#t>
</#if><#t>
<#if m.nullableChanged?? && m.nullableChanged>
<#if isPostgres>
ALTER TABLE ${quoteIdent(table)}
  ALTER COLUMN ${quoteIdent(m.name)}
  <#if m.newNullable?? && m.newNullable>DROP NOT NULL<#else>SET NOT NULL</#if>;
<#elseif isMySql || isMariaDB>
<#assign myColType = (m.newType!m.type!m.oldType)!"" >
<#if myColType?has_content>
ALTER TABLE ${quoteIdent(table)}
  MODIFY COLUMN ${quoteIdent(m.name)} ${myColType}
  <#if m.newNullable?? && !m.newNullable> NOT NULL<#else> NULL</#if>
;
</#if><#t>
<#elseif isMsSql>
<#assign msColType = (m.newType!m.type!m.oldType)!"" >
<#if msColType?has_content>
ALTER TABLE ${quoteIdent(table)}
  ALTER COLUMN ${quoteIdent(m.name)} ${msColType}<#if m.newNullable??><#if m.newNullable> NULL<#else> NOT NULL</#if></#if>;
</#if>
</#if>
</#if><#t>
<#if m.defaultChanged?? && m.defaultChanged>
<#if isPostgres>
ALTER TABLE ${quoteIdent(table)}
  ALTER COLUMN ${quoteIdent(m.name)}
  <#if m.newDefault?? && m.newDefault?has_content>
    SET DEFAULT ${m.newDefault}
  <#else>
    DROP DEFAULT
  </#if>;
<#elseif isMySql || isMariaDB>
ALTER TABLE ${quoteIdent(table)}
  ALTER COLUMN ${quoteIdent(m.name)}
  <#if m.newDefault?? && m.newDefault?has_content>
    SET DEFAULT ${m.newDefault}
  <#else>
    DROP DEFAULT
  </#if>;
<#elseif isMsSql>
DECLARE @df_name_${m_index} sysname;
SELECT @df_name_${m_index} = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c
  ON c.default_object_id = dc.object_id
WHERE dc.parent_object_id = OBJECT_ID(N'${table}')
  AND c.name = N'${m.name}';

IF @df_name_${m_index} IS NOT NULL
BEGIN
  EXEC(N'ALTER TABLE ${quoteIdent(table)} DROP CONSTRAINT [' + @df_name_${m_index} + N']');
END;
<#if m.newDefault?? && m.newDefault?has_content>
ALTER TABLE ${quoteIdent(table)}
  ADD CONSTRAINT df_${table}_${m.name}
  DEFAULT ${m.newDefault} FOR ${quoteIdent(m.name)};
</#if><#t>
</#if><#t>
</#if><#t>
<#if m.uniqueChanged?? && m.uniqueChanged>
<#if isPostgres>
ALTER TABLE ${quoteIdent(table)}
  DROP CONSTRAINT IF EXISTS uk_${table}_${m.name};
<#elseif isMySql || isMariaDB>
SET @uk_exists_${m_index} := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name   = '${table}'
    AND index_name   = 'uk_${table}_${m.name}'
);
SET @sql_uk_${m_index} := IF(
  @uk_exists_${m_index} > 0,
  'ALTER TABLE ${quoteIdent(table)} DROP INDEX ${quoteIdent("uk_${table}_${m.name}")}',
  'DO 0'
);
PREPARE stmt_drop_uk_${m_index} FROM @sql_uk_${m_index};
EXECUTE stmt_drop_uk_${m_index};
DEALLOCATE PREPARE stmt_drop_uk_${m_index};
<#elseif isMsSql>
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE [name] = 'uk_${table}_${m.name}')
BEGIN
  ALTER TABLE ${quoteIdent(table)} DROP CONSTRAINT uk_${table}_${m.name};
END;
</#if><#t>
<#if m.newUnique?? && m.newUnique>
ALTER TABLE ${quoteIdent(table)}
  ADD CONSTRAINT uk_${table}_${m.name}
  UNIQUE (${quoteIdent(m.name)});
</#if><#t>
</#if><#t>
</#list>
</#if><#t>
<#if pkChanged?? && pkChanged && newPk?has_content>
<#if isPostgres>
ALTER TABLE ${quoteIdent(table)}
  DROP CONSTRAINT IF EXISTS pk_${table};
<#elseif isMySql || isMariaDB>
SET @pk_exists := (
  SELECT COUNT(1)
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = '${table}'
    AND constraint_type = 'PRIMARY KEY'
);
SET @sql_pk := IF(
  @pk_exists > 0,
  'ALTER TABLE ${quoteIdent(table)} DROP PRIMARY KEY',
  'DO 0'
);
PREPARE stmt_drop_pk FROM @sql_pk;
EXECUTE stmt_drop_pk;
DEALLOCATE PREPARE stmt_drop_pk;
<#elseif isMsSql>
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE [name] = 'pk_${table}')
BEGIN
  ALTER TABLE ${quoteIdent(table)} DROP CONSTRAINT pk_${table};
END;
</#if><#t>
ALTER TABLE ${quoteIdent(table)}
  ADD CONSTRAINT pk_${table}
  PRIMARY KEY (
    <#list newPk as k>${quoteIdent(k)}<#if k_has_next>, </#if></#list>
  );
</#if><#t>
<#if removedFks?has_content>
<#list removedFks as fk>
<#if isPostgres>
ALTER TABLE ${quoteIdent(table)}
  DROP CONSTRAINT IF EXISTS fk_${table}_${fk.column};
<#elseif isMySql || isMariaDB>
SET @fk_exists_${fk_index} := (
  SELECT COUNT(1)
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = '${table}'
    AND constraint_type = 'FOREIGN KEY'
    AND constraint_name = 'fk_${table}_${fk.column}'
);
SET @sql_fk_${fk_index} := IF(
  @fk_exists_${fk_index} > 0,
  'ALTER TABLE ${quoteIdent(table)} DROP FOREIGN KEY fk_${table}_${fk.column}',
  'DO 0'
);
PREPARE stmt_drop_fk_${fk_index} FROM @sql_fk_${fk_index};
EXECUTE stmt_drop_fk_${fk_index};
DEALLOCATE PREPARE stmt_drop_fk_${fk_index};
<#elseif isMsSql>
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE [name] = 'fk_${table}_${fk.column}')
BEGIN
  ALTER TABLE ${quoteIdent(table)} DROP CONSTRAINT fk_${table}_${fk.column};
END;
</#if><#t>
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
<#if isPostgres>
ALTER TABLE ${quoteIdent(table)} ALTER COLUMN ${quoteIdent("created_at")} TYPE ${auditCreatedType};
ALTER TABLE ${quoteIdent(table)} ALTER COLUMN ${quoteIdent("updated_at")} TYPE ${auditUpdatedType};
<#elseif isMySql || isMariaDB>
ALTER TABLE ${quoteIdent(table)} MODIFY COLUMN ${quoteIdent("created_at")} ${auditCreatedType} NOT NULL DEFAULT ${auditNowExpr};
ALTER TABLE ${quoteIdent(table)} MODIFY COLUMN ${quoteIdent("updated_at")} ${auditUpdatedType} NOT NULL DEFAULT ${auditNowExpr};
<#elseif isMsSql>
ALTER TABLE ${quoteIdent(table)} ALTER COLUMN ${quoteIdent("created_at")} ${auditCreatedType} NOT NULL;
ALTER TABLE ${quoteIdent(table)} ALTER COLUMN ${quoteIdent("updated_at")} ${auditUpdatedType} NOT NULL;
</#if>
</#if><#t>