package com.example.fullfeatures;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.example.fullfeatures.configuration.CacheConfiguration;

@SpringBootApplication
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = CacheConfiguration.class
        )
)
public class SpringCrudGeneratorFullFeaturesItApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCrudGeneratorFullFeaturesItApplication.class, args);
    }
}
