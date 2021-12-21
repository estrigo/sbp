package kz.spt.lib.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ChangePasswordDto {
    private String userName;
    private String oldPassword;
    private String newPassword;
}
