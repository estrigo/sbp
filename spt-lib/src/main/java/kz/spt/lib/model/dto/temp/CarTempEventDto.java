package kz.spt.lib.model.dto.temp;

import javax.validation.constraints.NotNull;

public class CarTempEventDto {

    @NotNull
    public CarTempReqBodyDto body;

    @NotNull
    public CarTempReqFileDto file;
}
