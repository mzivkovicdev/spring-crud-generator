import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import graphql.scalars.ExtendedScalars;

@Configuration
public class GraphQlConfiguration {

    @Bean
    RuntimeWiringConfigurer scalarWiring() {
        return builder -> builder
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.Json);
    }
}
