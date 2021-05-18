package pl.mwa;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.BasicAuth;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo()).select()
                .apis(RequestHandlerSelectors.basePackage("pl.mwa"))
                .paths(PathSelectors.any())
                .build()
                .securitySchemes(Arrays.asList(new BasicAuth("basic")));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("Simple Post and Tags App with swagger")
                .description("Boot Demo with Swagger")
                .version("1.0.0").build();
    }
}

