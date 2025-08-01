<#list fields as field>
    <#if field.id>
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)</#if><#if field.type?lower_case == "enum">
    @Enumerated(EnumType.STRING)</#if>
    private ${field.resolvedType} ${field.name};
</#list>
