<#assign uncapModelName = strippedModelName?uncap_first>
<#assign modelName = strippedModelName?cap_first>
get:
      parameters:
      - in: query
        name: pageNumber
        description: Page number
        required: true
        schema:
          type: string
      - in: query
        name: pageSize
        description: Page size
        required: true
        schema:
          type: string
      
      summary: Get the ${uncapModelName}s
      tags:
          - "${modelName}"
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
                  size:
                    type: integer
                  number:
                    type: integer
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/${modelName}'