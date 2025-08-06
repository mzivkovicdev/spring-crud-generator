<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign mapperClass = strippedModelName?cap_first + "Mapper">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

${projectImports}
@RestController
@RequestMapping("/api/v1/${uncapModelName}s")
public class ${className} {

    private final ${mapperClass} mapper = Mappers.getMapper(${mapperClass}.class);

    private final ${serviceClass} ${serviceField};

    public ${className}(final ${serviceClass} ${serviceField}) {
        this.${serviceField} = ${serviceField};
    }

    <#if createResource?? && createResource?has_content>${createResource}</#if>
    <#if getResource?? && getResource?has_content>${getResource}</#if>
    <#if getAllResources?? && getAllResources?has_content>${getAllResources}</#if>
    <#if updateResource?? && updateResource?has_content>${updateResource}</#if>
    <#if deleteResource?? && deleteResource?has_content>${deleteResource}</#if>
}