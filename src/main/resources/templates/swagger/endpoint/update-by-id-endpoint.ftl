<#assign uncapModelName = strippedModelName?uncap_first>
<#assign capModelName = strippedModelName?cap_first>
<#assign modelName = strippedModelName?cap_first + "Payload">
put:
      summary: Update the ${uncapModelName} by ${idField}
      tags:
          - "${capModelName}"
      description: Update the ${uncapModelName} by ${idField}
      operationId: ${uncapModelName}sIdPut
      requestBody:
          required: true
          description: Request modifying ${uncapModelName}.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/${modelName}'
      responses:
          '200':
            description: Found ${uncapModelName} by provided ${uncapModelName} ID.
            content:
              application/json:
                schema:
                  type: object
                  $ref: '#/components/schemas/${modelName}'