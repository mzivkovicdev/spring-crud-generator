<#assign uncapModelName = strippedModelName?uncap_first>
<#assign capModelName = strippedModelName?cap_first>
<#assign idProperty = id.type>
delete:
      summary: Bulk delete ${uncapModelName}s
      tags:
          - "${capModelName}"
      description: Bulk delete ${uncapModelName}s
      operationId: ${uncapModelName}sBulkDelete
      requestBody:
        required: true
        description: Request deleting multiple ${uncapModelName}s.
        content:
          application/json:
            schema:
              type: array
              items:
                type: ${idProperty}
                <#if id.format??>format: ${id.format}</#if>
      responses:
        '204':
          description: Deleted ${uncapModelName}s.