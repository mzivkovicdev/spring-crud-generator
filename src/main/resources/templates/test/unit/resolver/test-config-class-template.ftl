import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import graphql.scalars.ExtendedScalars;

@TestConfiguration
public class ResolverTestConfiguration {

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
