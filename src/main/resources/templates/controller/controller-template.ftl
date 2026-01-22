<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign businessServiceClass = strippedModelName?cap_first + "BusinessService">
<#assign businessServiceField = strippedModelName?uncap_first + "BusinessService">
<#assign mapperClass = strippedModelName?cap_first + "RestMapper">
<#assign mapperField = strippedModelName?cap_first + "Mapper">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#if !swagger>
import jakarta.validation.Valid;

</#if><#t>
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;<#if !swagger>
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;</#if>
import org.springframework.web.bind.annotation.RequestMapping;<#if !swagger>
import org.springframework.web.bind.annotation.RequestParam;</#if>
import org.springframework.web.bind.annotation.RestController;

${projectImports}
@RestController
<#if swagger>@RequestMapping("${basePath}")<#else>@RequestMapping("${basePath}/${uncapModelName}s")</#if>
public class ${className}<#if swagger> implements ${strippedModelName}sApi</#if> {

    private final ${mapperClass} ${mapperField?uncap_first} = Mappers.getMapper(${mapperClass}.class);
    <#list jsonFields as jsonField>
    <#assign jsonFieldMapperClass = jsonField?cap_first + "RestMapper">
    <#assign jsonFieldMapper = jsonField?cap_first + "Mapper">
    private final ${jsonFieldMapperClass} ${jsonFieldMapper?uncap_first} = Mappers.getMapper(${jsonFieldMapperClass}.class);
    </#list>

    private final ${serviceClass} ${serviceField};
    <#if relations>
    private final ${businessServiceClass} ${businessServiceField};
    </#if>

    public ${className}(final ${serviceClass} ${serviceField}<#if relations>, final ${businessServiceClass} ${businessServiceField}</#if>) {
        this.${serviceField} = ${serviceField};<#if relations>
        this.${businessServiceField} = ${businessServiceField};</#if>
    }

    <#if createResource?? && createResource?has_content>${createResource}</#if>
    <#if getResource?? && getResource?has_content>${getResource}</#if>
    <#if getAllResources?? && getAllResources?has_content>${getAllResources}</#if>
    <#if updateResource?? && updateResource?has_content>${updateResource}</#if>
    <#if deleteResource?? && deleteResource?has_content>${deleteResource}</#if>
    <#if addResourceRelation?? && addResourceRelation?has_content>${addResourceRelation}</#if><#if removeResourceRelation?? && removeResourceRelation?has_content>${removeResourceRelation}</#if>
}