<#function quoteIdent ident>
    <#assign s = (ident!"" )>
    <#if db == "MYSQL">
        <#return "`${s}`">
    <#elseif db == "MSSQL">
        <#return "[${s}]">
    <#elseif db == "POSTGRESQL">
        <#return "\"${s}\"">
    </#if>
    <#return "\"${s}\"">
</#function>