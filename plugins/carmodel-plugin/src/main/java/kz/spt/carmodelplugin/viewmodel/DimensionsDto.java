package kz.spt.carmodelplugin.viewmodel;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DimensionsDto {

    private Long id;
    private String carClassification;
    private String updatedBy;
    private LocalDateTime updatedTime;

}
