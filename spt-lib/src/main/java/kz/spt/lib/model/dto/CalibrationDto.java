package kz.spt.lib.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationDto {
    private Long id;
    private Long cameraId;
    private String ip;
    private String json;
}
