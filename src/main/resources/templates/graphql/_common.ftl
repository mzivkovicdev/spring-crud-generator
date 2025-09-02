<#function isJson field>
  <#return field.type?matches("JSONB?\\[.+\\]")>
</#function>

<#function jsonInnerType field>
  <#-- backref $1 hvata unutrašnji deo -->
  <#return field.type?replace("^JSONB?\\[(.+)]$", "$1", "r")>
</#function>

<#function mapScalarType t>
  <#assign T = t?trim>
  <#if T == "UUID">
    <#return "UUID">
  <#elseif T == "String">
    <#return "String">
  <#elseif T == "Integer" || T == "Long" || T == "Short" || T == "Byte">
    <#return "Int">
  <#elseif T == "Float" || T == "Double" || T == "BigDecimal">
    <#return "Float">
  <#elseif T == "Boolean">
    <#return "Boolean">
  <#elseif T == "LocalDate">
    <#return "Date">
  <#elseif T == "LocalDateTime" || T == "OffsetDateTime" || T == "Instant">
    <#return "DateTime">
  <#else>
    <#-- Fallback: tretiraj kao referencirani tip/enum -->
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

<#function gqlFieldType field>
  <#if isJson(field)>
    <#return jsonInnerType(field)>
  <#else>
    <#return mapScalarType(field.type)>
  </#if>
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