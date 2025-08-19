<#assign modelName = strippedModelName?cap_first>
${modelName}:
      description: ${description}
      type: object
      properties:
        <#list properties as property>
        ${property.name}:
          <#if property.description??>description: ${property.description}</#if>
          type: ${property.type}
          <#if property.format??>format: ${property.format}</#if>
          <#if property.enum??>
          enum:
            <#list property.enum as ev>
            - ${ev}
            </#list>
          </#if>
        </#list>