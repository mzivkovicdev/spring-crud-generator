<#function isJson field>
  <#return field.type?matches("^JSONB?<.+>$")>
</#function>

<#function jsonInnerType field isInput>
  <#assign inner = field.type?replace("^JSONB?<(.+)>$", "$1", "r")>
  <#if isInput?? && isInput>
    <#return inner + "Input">
  <#else>
    <#return inner>
  </#if>
</#function>


<#function mapScalarType t>
  <#assign T = t?trim>
  <#if T == "UUID">
    <#return "UUID">
  <#elseif T == "String">
    <#return "String">
  <#elseif T == "Long">
    <#return "Long">
  <#elseif T == "Integer" || T == "Short" || T == "Byte">
    <#return "Int">
  <#elseif T == "Float">
    <#return "Float">
  <#elseif T == "Double" || T == "BigDecimal">
    <#return "BigDecimal">
  <#elseif T == "Boolean">
    <#return "Boolean">
  <#elseif T == "LocalDate">
    <#return "Date">
  <#elseif T == "LocalDateTime" || T == "OffsetDateTime" || T == "Instant">
    <#return "DateTime">
  <#else>
    <#return T>
  </#if>
</#function>

<#function isNonNull field>
  <#if (field.id?has_content)>
    <#return true>
  </#if>
  <#if field.column?has_content && field.column.nullable?has_content && (field.column.nullable == false)>
    <#return true>
  </#if>
  <#return false>
</#function>

<#function gqlFieldType field isInput>
  <#if isJson(field)>
    <#return jsonInnerType(field, isInput)>
  </#if>

  <#if isCollectionType(field.type)>
    <#assign inner = collectionInnerType(field.type)>
    <#assign gqlInner = mapScalarType(inner)>
    <#return "[" + gqlInner + "]">
  </#if>

  <#return mapScalarType(field.type)>
</#function>

<#function isCollectionType typeStr>
  <#assign T = typeStr?trim>
  <#return (T?starts_with("List<") || T?starts_with("Set<"))>
</#function>

<#function collectionInnerType typeStr>
  <#assign T = typeStr?trim>
  <#assign inner = T?keep_after("<")?keep_before_last(">")?trim>
  <#return inner>
</#function>

<#function withNullability baseType field>
  <#if isNonNull(field)>
    <#return baseType + "!">
  <#else>
    <#return baseType>
  </#if>
</#function>

<#function isEnum field>
  <#return (field.type?string == "Enum")>
</#function>

<#function hasRelation field>
  <#return field.relation?has_content>
</#function>

<#function relType field>
  <#if field.relation?has_content && field.relation.type?has_content>
    <#return field.relation.type>
  </#if>
  <#return "">
</#function>

<#function isToOne field>
  <#assign t = relType(field)>
  <#return t == "OneToOne" || t == "ManyToOne">
</#function>

<#function isToMany field>
  <#assign t = relType(field)>
  <#return t == "OneToMany" || t == "ManyToMany">
</#function>

<#function relTargetGqlType field>
  <#return field.type>
</#function>

<#function relInputRefName field>
  <#if isToMany(field)>
    <#return field.name + "Ids">
  <#else>
    <#return field.name + "Id">
  </#if>
</#function>

<#function relNonNull field>
  <#if field.column?has_content && field.column.nullable?has_content>
    <#return (field.column.nullable == false)>
  </#if>
  <#return false>
</#function>