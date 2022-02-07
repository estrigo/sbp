package kz.spt.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties("cors.allowed")
@Getter
@Setter
public class CorsAllowedOrigins {

    private List<String> origins;
}
