@Service
public class ${className} {

    private static final Logger LOGGER = LoggerFactory.getLogger(${className}.class);

    private final ${modelName}Repository repository;

    public ${className}(final ${modelName}Repository repository) {
        this.repository = repository;
    }
    
    <#if getByIdMethod?? && getByIdMethod?has_content>${getByIdMethod}</#if>
    <#if getAllMethod?? && getAllMethod?has_content>${getAllMethod}</#if>
    <#if createMethod?? && createMethod?has_content>${createMethod}</#if>
    <#if updateMethod?? && updateMethod?has_content>${updateMethod}</#if>
    <#if deleteMethod?? && deleteMethod?has_content>${deleteMethod}</#if>
    <#if addRelationMethod?? && addRelationMethod?has_content>${addRelationMethod}</#if><#if removeRelationMethod?? && removeRelationMethod?has_content>${removeRelationMethod}</#if><#if getAllByIds?? && getAllByIds?has_content>${getAllByIds}</#if><#if getReferenceById?? && getReferenceById?has_content>${getReferenceById}</#if>
}