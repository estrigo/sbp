package kz.spt.billingplugin.model.dto.rekassa;

import lombok.Data;

@Data
public class AuthRequestDTO {
    String number;
    String password;
}
