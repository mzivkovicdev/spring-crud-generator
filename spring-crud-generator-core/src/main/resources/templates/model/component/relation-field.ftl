    <#assign args = []>
    <#if field.relation.fetch??>
    <#assign args = args + ["fetch = FetchType." + field.relation.fetch]>
    </#if>
    <#if field.relation.cascade??>
    <#assign args = args + ["cascade = CascadeType." + field.relation.cascade]>
    </#if>
    <#assign supportsOrphanRemoval = field.relation.type == "OneToOne" || field.relation.type == "OneToMany">
    <#assign orphanRemovalEnabled = field.relation.orphanRemoval?? && field.relation.orphanRemoval>
    <#if supportsOrphanRemoval && orphanRemovalEnabled>
    <#assign args = args + ["orphanRemoval = true"]>
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
    <#assign joinColumnArgs = ["name = \"" + (field.relation.joinColumn!field.name + "_id") + "\""]>
    <#if field.column??>
    <#if field.column.unique??>
    <#assign joinColumnArgs = joinColumnArgs + ["unique = " + field.column.unique?c]>
    </#if>
    <#if field.column.nullable??>
    <#assign joinColumnArgs = joinColumnArgs + ["nullable = " + field.column.nullable?c]>
    </#if>
    <#if field.column.insertable??>
    <#assign joinColumnArgs = joinColumnArgs + ["insertable = " + field.column.insertable?c]>
    </#if>
    <#if field.column.updateable??>
    <#assign joinColumnArgs = joinColumnArgs + ["updatable = " + field.column.updateable?c]>
    </#if>
    </#if>
    @JoinColumn(${joinColumnArgs?join(", ")})
    </#if>
