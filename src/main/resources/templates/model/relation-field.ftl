    <#assign args = []>
    <#if field.relation.fetch??>
    <#assign args = args + ["fetch = FetchType." + field.relation.fetch]>
    </#if>
    <#if field.relation.cascade??>
    <#assign args = args + ["cascade = CascadeType." + field.relation.cascade]>
    </#if>
    @${field.relation.type}(${args?join(", ")})
    @JoinColumn(name = "${field.relation.joinColumn!field.name + "_id"}")
    