type: object
description: ${description}
properties:
<#list properties as p>
  ${p.name}:
  <#if p["$ref"]??>
    $ref: '${p["$ref"]}'
  <#else>
    type: ${p.type}
    <#if p.format??>format: ${p.format}</#if>
    <#if p.items??>
    items:
      <#if p.items["$ref"]??>
        $ref: '${p.items["$ref"]}'
      <#else>
        type: ${p.items.type}
        <#if p.items.format??>format: ${p.items.format}</#if>
      </#if>
    </#if>
    <#if p.enum??>
    enum:
      <#list p.enum as ev>
      - ${ev}
      </#list>
    </#if>
  </#if>
    <#if p.description??>description: ${p.description}</#if>
</#list>
<#if required?has_content>
required:
<#list required as r>
  - ${r}
</#list>
</#if>