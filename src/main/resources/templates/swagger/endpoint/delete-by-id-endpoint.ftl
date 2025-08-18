<#assign uncapModelName = strippedModelName?uncap_first>
<#assign modelName = strippedModelName?cap_first>
delete:
      summary: Delete the ${uncapModelName} by ${idField}
      tags:
          - "${modelName}"
      description: Delete the ${uncapModelName} by ${idField}
      operationId: ${uncapModelName}sIdDelete
      responses:
          '204':
            description: Deleted ${uncapModelName}.