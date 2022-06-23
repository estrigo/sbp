package kz.spt.lib.model.dto.carmen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarmenAuthRequest {
    private String User;
    private String Password;
}
