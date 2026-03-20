<#assign uncapModelName = strippedModelName?uncap_first>
<#assign capModelName = strippedModelName?cap_first>
<#assign requestModelName = strippedModelName?cap_first + "CreatePayload">
<#assign responseModelName = strippedModelName?cap_first + "Payload">
post:
      summary: Bulk create ${uncapModelName}s
      tags:
          - "${capModelName}"
      description: Bulk create ${uncapModelName}s
      operationId: ${uncapModelName}sBulkPost
      requestBody:
        required: true
        description: Request creating multiple ${uncapModelName}s.
        content:
          application/json:
            schema:
              type: array
              items:
                $ref: '#/components/schemas/${requestModelName}'
      responses:
        '200':
          description: Created ${uncapModelName}s.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/${responseModelName}'
