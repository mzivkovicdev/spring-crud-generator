<#assign uncapModelName = strippedModelName?uncap_first>
<#assign capModelName = strippedModelName?cap_first>
<#assign modelName = strippedModelName?cap_first + "Payload">
get:
      parameters:
      - in: query
        name: pageNumber
        description: Page number
        required: true
        schema:
          type: integer
      - in: query
        name: pageSize
        description: Page size
        required: true
        schema:
          type: integer
      <#if sortEnabled?? && sortEnabled>
      - in: query
        name: sortBy
        description: Sort field. Allowed values are ${sortAllowedFieldsCsv}.
        required: false
        schema:
          type: string
          enum:
          <#list sortAllowedFields as sortField>
            - ${sortField}
          </#list>
      - in: query
        name: sortDirection
        description: Sort direction. Allowed values are ASC and DESC. Used only when sortBy is provided.
        required: false
        schema:
          type: string
          enum:
            - ASC
            - DESC
          default: ${sortDefaultDirection}
      </#if>
      
      summary: Get the ${uncapModelName}s
      tags:
          - "${capModelName}"
      description: Get the ${uncapModelName}s
      operationId: ${uncapModelName}sGet
      responses:
        '200':
          description: Found ${uncapModelName}s.
          content:
            application/json:
              schema:
                properties:
                  totalPages:
                    type: integer
                  totalElements:
                    type: integer
                    format: int64
                  size:
                    type: integer
                  number:
                    type: integer
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/${modelName}'
