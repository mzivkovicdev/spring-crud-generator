<#assign uncapModelName = strippedModelName?uncap_first>
<#assign capModelName = strippedModelName?cap_first>
<#assign modelName = strippedModelName?cap_first + "Payload">

  <#list relations as rel>
  <#assign relationField = rel.strippedModelName?uncap_first>
  <#assign relationInput = rel.strippedModelName?cap_first + "Input">
  <#assign relType = rel.relationType?upper_case>
  <#assign relatedIdParam = rel.relatedIdParam>
  /${uncapModelName}s/{${idField}}/${relationField}s:
    parameters:
      - in: path
        name: ${idField}
        description: ${idDescription}
        required: true
        schema:
          type: ${id.type}
          <#if id.format??>format: ${id.format}</#if>
    post:
      summary: Add ${relationField} to ${uncapModelName}
      tags:
        - ${capModelName}
      operationId: ${uncapModelName}sId${relationField?cap_first}sPost
      requestBody:
        required: true
        description: Request adding ${rel.strippedModelName} to ${modelName}
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/${relationInput}'
      responses:
        '200':
          description: Added ${rel.strippedModelName} to ${modelName}
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/${modelName}'
  <#if relType == "ONETOONE" || relType == "MANYTOONE">
    delete:
      summary: Remove ${relationField} from ${uncapModelName}
      tags:
        - ${capModelName}
      operationId: ${uncapModelName}sId${relationField?cap_first}sDelete
      responses:
        '204':
          description: Removed ${relationField} from ${uncapModelName}
  <#else>
  /${uncapModelName}s/{${idField}}/${relationField}s/{${relatedIdParam}}:
    parameters:
      - in: path
        name: ${idField}
        description: ${idDescription}
        required: true
        schema:
          type: ${id.type}
          <#if id.format??>format: ${id.format}</#if>
      - in: path
        name: ${relatedIdParam}
        description: ID of related ${rel.strippedModelName} to remove
        required: true
        schema:
          type: ${rel.relatedId.type}
          <#if rel.relatedId.format??>format: ${rel.relatedId.format}</#if>
    delete:
      summary: Remove ${relationField} from ${uncapModelName}
      tags:
        - ${capModelName}
      operationId: ${uncapModelName}sId${relationField?cap_first}sDelete
      responses:
        '204':
          description: Removed ${rel.strippedModelName} from ${modelName}
  </#if>
  </#list>