<#if baseImports?has_content>
${baseImports}
</#if><#t>
<#if openInViewEnabled?? && !openInViewEnabled && hasLazyFields?? && hasLazyFields>
import org.springframework.data.jpa.repository.EntityGraph;
</#if><#t>
import org.springframework.data.jpa.repository.JpaRepository;

${projectImports}
public interface ${className} extends JpaRepository<${modelName}, ${idType}> {

    <#if openInViewEnabled?? && !openInViewEnabled && hasLazyFields?? && hasLazyFields>
    @EntityGraph(value = "${entityGraphName}", type = EntityGraph.EntityGraphType.LOAD)
    Optional<${modelName}> findById(final ${idType} ${idField});
    
    </#if><#t>
}