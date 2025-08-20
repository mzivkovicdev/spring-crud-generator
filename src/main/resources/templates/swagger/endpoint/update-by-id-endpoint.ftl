<#assign uncapModelName = strippedModelName?uncap_first>
<#assign modelName = strippedModelName?cap_first>
put:
      summary: Update the ${uncapModelName} by ${idField}
      tags:
          - "${modelName}"
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