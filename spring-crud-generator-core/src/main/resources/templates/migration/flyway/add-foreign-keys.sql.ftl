<#include "_common.ftl">
<#list fks as fk>
ALTER TABLE ${quoteIdent(fk.table)}
ADD CONSTRAINT ${fk.name}
FOREIGN KEY (${quoteIdent(fk.column)})
REFERENCES ${quoteIdent(fk.refTable)}(${quoteIdent(fk.refColumn)})<#if fk.onDelete??> ON DELETE ${fk.onDelete}</#if><#if fk.onUpdate??> ON UPDATE ${fk.onUpdate}</#if><#if fk.deferrable?? && fk.deferrable> DEFERRABLE INITIALLY DEFERRED</#if>;
</#list>
