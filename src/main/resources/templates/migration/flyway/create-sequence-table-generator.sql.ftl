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

INSERT INTO ${name}(${pkColumnName}, ${valueColumnName})
    VALUES ('${pkColumnValue}', ${initialValue})
    ON CONFLICT DO NOTHING;
</#if>