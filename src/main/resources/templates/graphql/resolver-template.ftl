<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign mapperClass = strippedModelName?cap_first + "Mapper">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

${projectImports}
@Controller
public class ${className} {

    private final ${mapperClass} ${mapperClass?uncap_first} = Mappers.getMapper(${mapperClass}.class);

    private final ${serviceClass} ${serviceField};

    public ${className}(final ${serviceClass} ${serviceField}) {
        this.${serviceField} = ${serviceField};
    }

    ${queries}
    ${mutations}

}