<#assign uncapModelName = strippedModelName?uncap_first>
<#assign modelName = strippedModelName?cap_first>
get:
      summary: Get the ${uncapModelName} by ${idField}
      tags:
          - "${modelName}"
      description: Get the ${uncapModelName} by ${idField}
      operationId: ${uncapModelName}sIdGet
      responses:
          '200':
            description: Found ${uncapModelName}.
            content:
              application/json:
                schema:
                  type: object
                  $ref: '#/components/schemas/${modelName}'