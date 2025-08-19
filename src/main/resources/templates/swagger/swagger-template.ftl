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
    parameters:
    - in: query
      name: pageNumber
      description: Page number
      required: true
      schema:
        type: string
    - in: query
      name: pageSize
      description: Page size
      required: true
      schema:
        type: string
    
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

components:
  schemas:
<#list schemaNames as s>
    ${s?cap_first}:
      $ref: './components/schemas/${s}.yaml'
</#list>
