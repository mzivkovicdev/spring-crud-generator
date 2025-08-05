    <#assign args = []>
    <#if field.relation.fetch??>
    <#assign args = args + ["fetch = FetchType." + field.relation.fetch]>
    </#if>
    <#if field.relation.cascade??>
    <#assign args = args + ["cascade = CascadeType." + field.relation.cascade]>
    </#if>
    @${field.relation.type}(${args?join(", ")})
    <#if field.relation.type == "ManyToMany">
    @JoinTable(
        name = "${field.relation.joinTable.name}",
        joinColumns = @JoinColumn(name = "${field.relation.joinTable.joinColumn}"),
        inverseJoinColumns = @JoinColumn(name = "${field.relation.joinTable.inverseJoinColumn}")
    )
    </#if>
    <#if field.relation.type != "ManyToMany">
    @JoinColumn(name = "${field.relation.joinColumn!field.name + "_id"}")
    </#if>