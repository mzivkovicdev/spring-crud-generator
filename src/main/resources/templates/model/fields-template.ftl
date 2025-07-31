<#list fields as field>
    <#if field.id>
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)</#if>
    private ${field.type} ${field.name};
</#list>
