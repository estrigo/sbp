package kz.spt.lib.model.dto.jetson;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JetsonResponse {
    private String msg;
    private Boolean success;
    private Integer state;
}
