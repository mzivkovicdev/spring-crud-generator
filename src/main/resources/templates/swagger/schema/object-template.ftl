type: object
<#if description??>
description: ${description}
</#if>
<#if title??>
title: ${title}
</#if><#t>
properties:
<#list properties as p>
  ${p.name}:
  <#if p["$ref"]??>
    $ref: '${p["$ref"]}'
  <#else>
    type: ${p.type}
    <#if p.format??>
    format: ${p.format}
    </#if><#t>
    <#if p.nullable??>
    nullable: ${p.nullable?c}
    </#if><#t>
    <#if !p.items?? && p.minimum??>
    minimum: ${p.minimum?c}
    </#if><#t>
    <#if !p.items?? && p.maximum??>
    maximum: ${p.maximum?c}
    </#if><#t>
    <#if !p.items?? && p.minLength??>
    minLength: ${p.minLength?c}
    </#if><#t>
    <#if !p.items?? && p.maxLength??>
    maxLength: ${p.maxLength?c}
    </#if><#t>
    <#if p.items?? && p.uniqueItems??>
    uniqueItems: ${p.uniqueItems?c}
    </#if><#t>
    <#if p.items?? && p.minItems??>
    minItems: ${p.minItems?c}
    </#if><#t>
    <#if p.items?? && p.maxItems??>
    maxItems: ${p.maxItems?c}
    </#if><#t>
    <#if p.items??>
    items:
      <#if p.items["$ref"]??>
      $ref: '${p.items["$ref"]}'
      <#else>
      type: ${p.items.type}
      <#if p.items.format??>
      format: ${p.items.format}
      </#if><#t>
      </#if><#t>
    </#if><#t>
    <#if p.enum??>
    enum:
      <#list p.enum as ev>
      - ${ev}
      </#list>
    </#if><#t>
  </#if><#t>
    <#if p.description??>
    description: ${p.description}
    </#if><#t>
</#list>
  <#if auditEnabled?? && auditEnabled>
  createdAt:
    type: ${auditType.type}
    format: ${auditType.format}
    readOnly: true
    description: Timestamp when the resource was created
  updatedAt:
    type: ${auditType.type}
    format: ${auditType.format}
    readOnly: true
    description: Timestamp when the resource was updated
  </#if><#t>
<#if required?has_content>
required:
<#list required as r>
  - ${r}
</#list>
</#if>