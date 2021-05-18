package pl.mwa;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.thymeleaf.dialect.springdata.SpringDataDialect;

@Configuration
public class WebAppConfig extends WebMvcConfigurerAdapter {

	
    @Bean
    public SpringDataDialect springDataDialect() {
        return new SpringDataDialect();
    }

	
}
