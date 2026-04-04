<#assign uncapModelName = strippedModelName?uncap_first>
<#assign capModelName = strippedModelName?cap_first>
<#assign requestModelName = strippedModelName?cap_first + "CreatePayload">
<#assign responseModelName = strippedModelName?cap_first + "Payload">
post:
      summary: Create new ${uncapModelName}
      tags:
          - "${capModelName}"
      description: Create new ${uncapModelName}
      operationId: ${uncapModelName}sPost
      requestBody:
        required: true
        description: Request creating a new ${uncapModelName}.
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/${requestModelName}'
      responses:
        '200':
          description: Created new ${uncapModelName}.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/${responseModelName}'