<#include "_common.ftl">
CREATE TABLE<#if db != "MSSQL"> IF NOT EXISTS</#if> ${quoteIdent(joinTable)} (
  ${quoteIdent(left.column)} ${left.sqlType} NOT NULL,
  ${quoteIdent(right.column)} ${right.sqlType} NOT NULL,
  CONSTRAINT pk_${joinTable} PRIMARY KEY (${quoteIdent(left.column)}, ${quoteIdent(right.column)}),
  CONSTRAINT fk_${joinTable}_${left.column} FOREIGN KEY (${quoteIdent(left.column)}) REFERENCES ${quoteIdent(left.table)}(${quoteIdent(left.pkColumn)}),
  CONSTRAINT fk_${joinTable}_${right.column} FOREIGN KEY (${quoteIdent(right.column)}) REFERENCES ${quoteIdent(right.table)}(${quoteIdent(right.pkColumn)})
);
