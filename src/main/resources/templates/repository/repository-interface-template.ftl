<#if baseImports?has_content>
${baseImports}
</#if><#t>
<#if !openInViewEnabled && hasLazyFields>
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
</#if><#t>
import org.springframework.data.jpa.repository.JpaRepository;

${projectImports}
public interface ${className} extends JpaRepository<${modelName}, ${idType}> {

    <#if !openInViewEnabled && hasLazyFields>
    @EntityGraph(value = "${entityGraphName}", type = EntityGraph.EntityGraphType.LOAD)
    Optional<${modelName}> findById(final ${idType} ${idField});

    @EntityGraph(value = "${entityGraphName}", type = EntityGraph.EntityGraphType.LOAD)
    Page<${modelName}> findAll(final Pageable pageable);
    
    </#if><#t>
}