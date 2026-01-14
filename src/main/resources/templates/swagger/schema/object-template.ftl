type: object
description: ${description}
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
    <#if p.maxLength??>
    maxLength: ${p.maxLength?c}
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
<#if required?has_content>
required:
<#list required as r>
  - ${r}
</#list>
</#if>