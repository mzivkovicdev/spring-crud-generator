<#assign uncapModelName = strippedModelName?uncap_first>
<#assign capModelName = strippedModelName?cap_first>
<#assign modelName = strippedModelName?cap_first + "Payload">
delete:
      summary: Delete the ${uncapModelName} by ${idField}
      tags:
          - "${capModelName}"
      description: Delete the ${uncapModelName} by ${idField}
      operationId: ${uncapModelName}sIdDelete
      responses:
          '204':
            description: Deleted ${uncapModelName}.