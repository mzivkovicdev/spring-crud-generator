<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign mapperClass = strippedModelName?cap_first + "GraphQLMapper">
<#assign mapperField = strippedModelName?cap_first + "Mapper">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign businessServiceField = strippedModelName?uncap_first + "BusinessService">
<#assign businessServiceClass = strippedModelName?cap_first + "BusinessService">
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

${projectImports}
@Controller
public class ${className} {

    private final ${mapperClass} ${mapperField?uncap_first} = Mappers.getMapper(${mapperClass}.class);
    <#list jsonFields as jsonField>
    <#assign jsonFieldMapperClass = jsonField?cap_first + "GraphQLMapper">
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

    ${queries}
    ${mutations}

}