<#if sequence??>
CREATE SEQUENCE IF NOT EXISTS ${name}
    START WITH ${initialValue}
    INCREMENT BY ${allocationSize};
</#if>

<#if table??>
CREATE TABLE IF NOT EXISTS ${name} (
    ${pkColumnName} VARCHAR(255) PRIMARY KEY,
    ${valueColumnName} BIGINT
);

INSERT INTO ${name}(${pkColumnName}, ${valueColumnName})
    VALUES ('${pkColumnValue}', ${initialValue})
    ON CONFLICT DO NOTHING;
</#if>