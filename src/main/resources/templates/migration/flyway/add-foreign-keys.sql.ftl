<#list fks as fk>
ALTER TABLE ${fk.table}
ADD CONSTRAINT ${fk.name}
FOREIGN KEY (${fk.column})
REFERENCES ${fk.refTable}(${fk.refColumn})<#if fk.onDelete??> ON DELETE ${fk.onDelete}</#if><#if fk.onUpdate??> ON UPDATE ${fk.onUpdate}</#if><#if fk.deferrable?? && fk.deferrable> DEFERRABLE INITIALLY DEFERRED</#if>;
</#list>
