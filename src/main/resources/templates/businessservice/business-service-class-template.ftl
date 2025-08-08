@Service
public class ${className} {

    private static final Logger LOGGER = LoggerFactory.getLogger(${className}.class);

    <#list serviceClasses as serviceClass>
    private final ${serviceClass?cap_first} ${serviceClass?uncap_first};
    </#list>

    public ${className}(<#list serviceClasses as serviceClass>final ${serviceClass?cap_first} ${serviceClass?uncap_first}<#if serviceClass_has_next>, </#if></#list>) {
        <#list serviceClasses as serviceClass>
        this.${serviceClass?uncap_first} = ${serviceClass?uncap_first};
        </#list>
    }
    <#if createResource?? && createResource?has_content>${createResource}</#if>
    <#if addRelationMethod?? && addRelationMethod?has_content>${addRelationMethod}</#if>
    <#if removeRelationMethod?? && removeRelationMethod?has_content>${removeRelationMethod}</#if>
}