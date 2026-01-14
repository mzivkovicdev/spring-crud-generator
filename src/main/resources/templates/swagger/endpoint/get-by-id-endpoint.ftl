<#assign uncapModelName = strippedModelName?uncap_first>
<#assign capModelName = strippedModelName?cap_first>
<#assign modelName = strippedModelName?cap_first + "Payload">
get:
      summary: Get the ${uncapModelName} by ${idField}
      tags:
          - "${capModelName}"
      description: Get the ${uncapModelName} by ${idField}
      operationId: ${uncapModelName}sIdGet
      responses:
          '200':
            description: Found ${uncapModelName}.
            content:
              application/json:
                schema:
                  $ref: '#/components/schemas/${modelName}'