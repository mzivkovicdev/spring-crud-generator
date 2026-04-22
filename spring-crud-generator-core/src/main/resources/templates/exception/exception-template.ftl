public class ${className} extends <#if className == "EtArgumentException">IllegalArgumentException<#else>RuntimeException</#if> {

    public ${className}(final String message) {
        super(message);
    }
    
}