<#if isSpringBoot3>
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
<#else>
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
</#if>
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
<#if isSpringBoot3>
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
</#if><#t>

@Configuration
public class JacksonNullExclusionConfig {

    <#if isSpringBoot3>
    @Bean
    Jackson2ObjectMapperBuilderCustomizer jsonMapperCustomizer() {
        return builder -> builder.serializationInclusion(JsonInclude.Include.NON_NULL)
                .modulesToInstall(JavaTimeModule.class)
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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