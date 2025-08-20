<#assign modelName = strippedModelName?cap_first>
<#assign uncapModelName = strippedModelName?uncap_first>
openapi: 3.0.0

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
        description: ${idDescription}
        required: true
        schema:
          type: string
      
    ${getById}
    ${updateById}
    ${deleteById}
  <#if addRelations??>${addRelations}</#if>
components:
  schemas:
<#list schemaNames as s>
    ${s?cap_first}:
      $ref: './components/schemas/${s}.yaml'
</#list>
