<#if sequence?? && db != "MYSQL">
CREATE SEQUENCE IF NOT EXISTS ${name}
    START WITH ${initialValue}
    INCREMENT BY ${allocationSize};
</#if><#t>
<#if sequence?? && db == "MYSQL">
CREATE TABLE ${name} (
    next_val BIGINT NOT NULL
);
INSERT INTO ${name} (next_val) VALUES (${initialValue});
</#if><#t>
<#if table??>
CREATE TABLE IF NOT EXISTS ${name} (
    ${pkColumnName} VARCHAR(255) PRIMARY KEY,
    ${valueColumnName} BIGINT
);

<#if db == "POSTGRES">
INSERT INTO ${name}(${pkColumnName}, ${valueColumnName})
    VALUES ('${pkColumnValue}', ${initialValue})
    ON CONFLICT DO NOTHING;
</#if><#t>
<#if db == "MYSQL">
INSERT INTO ${name}(${pkColumnName}, ${valueColumnName})
    VALUES ('${pkColumnValue}', ${initialValue})
    ON DUPLICATE KEY UPDATE ${valueColumnName} = ${valueColumnName};
</#if><#t>
<#if db == "MSSQL">
IF NOT EXISTS (SELECT 1 FROM ${name} WHERE ${pkColumnName} = '${pkColumnValue}')
BEGIN
    INSERT INTO ${name}(${pkColumnName}, ${valueColumnName})
    VALUES ('${pkColumnValue}', ${initialValue});
END
</#if><#t>
</#if>