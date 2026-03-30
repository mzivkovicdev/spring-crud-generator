<#include "_common.ftl">
<#if sequence?? && db != "MYSQL">
CREATE SEQUENCE<#if db != "MSSQL"> IF NOT EXISTS</#if> ${quoteIdent(name)}
    START WITH ${initialValue}
    INCREMENT BY ${allocationSize};
</#if><#t>
<#if sequence?? && db == "MYSQL">
CREATE TABLE ${quoteIdent(name)} (
    ${quoteIdent("next_val")} BIGINT NOT NULL
);
INSERT INTO ${quoteIdent(name)} (${quoteIdent("next_val")}) VALUES (${initialValue});
</#if><#t>
<#if table??>
CREATE TABLE<#if db != "MSSQL"> IF NOT EXISTS</#if> ${quoteIdent(name)} (
    ${quoteIdent(pkColumnName)} VARCHAR(255) PRIMARY KEY,
    ${quoteIdent(valueColumnName)} BIGINT
);

<#if db == "POSTGRESQL">
INSERT INTO ${quoteIdent(name)}(${quoteIdent(pkColumnName)}, ${quoteIdent(valueColumnName)})
    VALUES ('${pkColumnValue}', ${initialValue})
    ON CONFLICT DO NOTHING;
</#if><#t>
<#if (db == "MYSQL")>
INSERT INTO ${quoteIdent(name)}(${quoteIdent(pkColumnName)}, ${quoteIdent(valueColumnName)})
    VALUES ('${pkColumnValue}', ${initialValue})
    ON DUPLICATE KEY UPDATE ${quoteIdent(valueColumnName)} = ${quoteIdent(valueColumnName)};
</#if><#t>
<#if (db == "MARIADB")>
INSERT INTO ${quoteIdent(name)}(${quoteIdent(pkColumnName)}, ${quoteIdent(valueColumnName)})
    VALUES ('${pkColumnValue}', ${initialValue})
    ON DUPLICATE KEY UPDATE ${quoteIdent(valueColumnName)} = ${quoteIdent(valueColumnName)};
</#if><#t>
<#if db == "MSSQL">
IF NOT EXISTS (SELECT 1 FROM ${quoteIdent(name)} WHERE ${quoteIdent(pkColumnName)} = '${pkColumnValue}')
BEGIN
    INSERT INTO ${quoteIdent(name)}(${quoteIdent(pkColumnName)}, ${quoteIdent(valueColumnName)})
    VALUES ('${pkColumnValue}', ${initialValue});
END
</#if><#t>
</#if>