package kz.spt.lib.model.dto;

import lombok.Data;

@Data
public class CarStateFilterDto {

    public String dateFromString;
    public String dateToString;
    public String plateNumber;
}
