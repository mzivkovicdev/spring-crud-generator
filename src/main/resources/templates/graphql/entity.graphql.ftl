<#include "_common.ftl">
<#list fields?filter(f -> isEnum(f)) as ef>
enum ${name}${ef.name?cap_first}Enum {
  <#list ef.values![] as v>
  ${v}
  </#list>
}
</#list>

type ${name} {
  <#list fields as field>
  <#if hasRelation(field)>
  <#assign tgt = relTargetGqlType(field)>
  <#if isToMany(field)>
  ${field.name}: [${tgt}!]<#if relNonNull(field)>!</#if>
  <#else>
  ${field.name}: ${tgt}<#if relNonNull(field)>!</#if>
  </#if>
  <#else>
  <#assign baseType = (isEnum(field))?then(name + field.name?cap_first + "Enum", gqlFieldType(field))>
  <#assign typeWithNull = withNullability(baseType, field)>
  ${field.name}: ${typeWithNull}
  </#if>
  </#list>
}

input ${name}CreateInput {
  <#list fields?filter(f -> !(f.id?has_content && f.id == true)) as field>
  <#if hasRelation(field)>
  <#assign refName = relInputRefName(field)>
  <#if isToMany(field)>
  ${refName}: [ID!]!
  <#else>
  ${refName}: ID!
  </#if>
  <#else>
  <#assign baseType = (isEnum(field))?then(name + field.name?cap_first + "Enum", gqlFieldType(field))>
  <#assign typeWithNull = (field.column?has_content && field.column.nullable?has_content && field.column.nullable == false)?then(baseType + "!", baseType)>
  ${field.name}: ${typeWithNull}
  </#if>
  </#list>
}

<#assign updatableFields = fields?filter(f -> !(f.id?has_content && f.id == true) && !hasRelation(f))>
<#if updatableFields?size gt 0>
input ${name}UpdateInput {
  <#list updatableFields as field>
  <#assign baseType = (isEnum(field))?then(name + field.name?cap_first + "Enum", gqlFieldType(field))>
  ${field.name}: ${baseType}
  </#list>
}
</#if>

extend type Query {
  ${name?uncap_first}ById(id: ID!): ${name}
  ${name?uncap_first}sPage(pageNumber: Int = 0, pageSize: Int = 20): Page
}

extend type Mutation {
  create${name}(input: ${name}CreateInput!): ${name}!
  update${name}(id: ID!, input: ${name}UpdateInput!): ${name}!
  delete${name}(id: ID!): Boolean!
  <#list fields?filter(f -> hasRelation(f)) as field>
  <#assign relCap = field.name?cap_first>
  add${relCap}(id: ID!, ${field.name}Id: ID!): ${name}!
  remove${relCap}(id: ID! <#if isToMany(field)>, ${field.name}Id: ID!</#if>): ${name}!
  </#list>
}