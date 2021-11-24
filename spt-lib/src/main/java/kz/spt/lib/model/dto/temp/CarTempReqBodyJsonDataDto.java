package kz.spt.lib.model.dto.temp;

import javax.validation.constraints.NotNull;

public class CarTempReqBodyJsonDataDto {

    @NotNull
    public String camera_id;

    @NotNull
    public CarTempReqBodyJsonDataResultDto[] results;
}
