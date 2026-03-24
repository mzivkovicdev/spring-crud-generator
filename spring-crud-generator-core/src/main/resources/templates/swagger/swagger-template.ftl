<#assign modelName = strippedModelName?cap_first>
<#assign uncapModelName = strippedModelName?uncap_first>
openapi: 3.1.0

info:
  title: ${modelName} API
  version: 1.0.0

servers:
  - url: http://localhost:8080/

tags:
  - name: "${modelName}"

paths:
  /${uncapModelName}s:
    ${create}
    ${getAll}

  /${uncapModelName}s/{${idField}}:
    parameters:
      - in: path
        name: ${idField}
        <#if idDescription?? && idDescription?has_content>
        description: ${idDescription}
        </#if><#t>
        required: true
        schema:
          type: ${id.type}
          <#if id.format??>format: ${id.format}</#if>
      
    ${getById}
    ${updateById}
    ${deleteById}
  <#if relationEndpoints??>${relationEndpoints}</#if>
components:
  schemas:
<#list schemaNames as s>
    ${s?cap_first}:
      $ref: './components/schemas/${s}.yaml'
</#list>
