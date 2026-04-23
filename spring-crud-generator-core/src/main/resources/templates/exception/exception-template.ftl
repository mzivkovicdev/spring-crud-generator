public class ${className} extends <#if className == "InvalidArgumentException">IllegalArgumentException<#else>RuntimeException</#if> {

    public ${className}(final String message) {
        super(message);
    }
    
}