package kz.spt.billingplugin.model.dto.webkassa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthRequestDTO {
    @JsonProperty("Login")
    String login;
    @JsonProperty("Password")
    String password;
}
