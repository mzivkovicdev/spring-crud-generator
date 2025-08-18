<#assign uncapModelName = strippedModelName?uncap_first>
<#assign modelName = strippedModelName?cap_first>
post:
      summary: Create new ${uncapModelName}
      tags:
          - "${modelName}"
      description: Create new ${uncapModelName}
      operationId: ${uncapModelName}sPost
      requestBody:
        required: true
        description: Request creating a new ${uncapModelName}.
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/${modelName}'
      responses:
        '200':
          description: Created new ${uncapModelName}.
          content:
            application/json:
              schema:
                type: object
                $ref: '#/components/schemas/${modelName}'