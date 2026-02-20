<#if isSpringBoot3>
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
<#else>
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
</#if>
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude;

@Configuration
public class JacksonNullExclusionConfig {

    <#if isSpringBoot3>
    @Bean
    Jackson2ObjectMapperBuilderCustomizer jsonMapperCustomizer() {
        return builder -> builder.serializationInclusion(JsonInclude.Include.NON_NULL);
    }
    <#else>
    @Bean
    JsonMapperBuilderCustomizer jsonMapperCustomizer() {
        return builder -> builder.changeDefaultPropertyInclusion(
            incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)
        );
    }
    </#if>
}