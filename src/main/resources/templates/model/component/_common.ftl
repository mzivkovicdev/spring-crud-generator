<#function collectionImpl fieldType>
  <#assign t = fieldType?trim>
  <#if t?starts_with("List<")>
    <#return "ArrayList">
  <#elseif t?starts_with("Set<")>
    <#return "HashSet">
  <#else>
    <#return "">
  </#if>
</#function>

<#function isCollection fieldType>
  <#assign t = fieldType?trim>
  <#if t?starts_with("List<") && t?ends_with(">")>
    <#return true>
  </#if>
  <#if t?starts_with("Set<") && t?ends_with(">")>
    <#return true>
  </#if>
  <#return false>
</#function>

<#function toSnakeCase s>
  <#if !s??>
    <#return "">
  </#if>

  <#assign x = s?trim?replace("-", "_")?replace(" ", "_")>
  <#assign x = x?replace("([a-z0-9])([A-Z])", "$1_$2", "r")>
  <#assign x = x?replace("([A-Z]+)([A-Z][a-z])", "$1_$2", "r")>
  <#return x?lower_case>
</#function>