CREATE TABLE<#if db != "MSSQL"> IF NOT EXISTS</#if> ${joinTable} (
  ${left.column} ${left.sqlType} NOT NULL,
  ${right.column} ${right.sqlType} NOT NULL,
  CONSTRAINT pk_${joinTable} PRIMARY KEY (${left.column}, ${right.column})
);
