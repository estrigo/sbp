package kz.spt.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "parking")
@Getter
@Setter
public class ParkingProperties {

    private Map<String, String> cameras;
}
