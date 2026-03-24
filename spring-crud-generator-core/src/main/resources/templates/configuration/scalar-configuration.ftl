import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import graphql.scalars.ExtendedScalars;
<#if auditEnabled?? && auditEnabled>
import graphql.schema.GraphQLScalarType;
</#if><#t>

@Configuration
public class GraphQlConfiguration {

    @Bean
    RuntimeWiringConfigurer scalarWiring(<#if auditEnabled?? && auditEnabled>final GraphQLScalarType dateTimeScalar</#if>) {
        return builder -> builder
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.Date)
                <#if auditEnabled?? && auditEnabled>.scalar(dateTimeScalar)<#else>.scalar(ExtendedScalars.DateTime)</#if>
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.Json);
    }
}
