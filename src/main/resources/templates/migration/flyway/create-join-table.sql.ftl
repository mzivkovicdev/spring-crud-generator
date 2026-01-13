CREATE TABLE<#if db != "MSSQL"> IF NOT EXISTS</#if> ${joinTable} (
  ${left.column} ${left.sqlType} NOT NULL,
  ${right.column} ${right.sqlType} NOT NULL,
  CONSTRAINT pk_${joinTable} PRIMARY KEY (${left.column}, ${right.column}),
  CONSTRAINT fk_${joinTable}_${left.column} FOREIGN KEY (${left.column}) REFERENCES ${left.table}(${left.pkColumn}),
  CONSTRAINT fk_${joinTable}_${right.column} FOREIGN KEY (${right.column}) REFERENCES ${right.table}(${right.pkColumn})
);
