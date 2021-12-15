package kz.spt.lib.model.dto;

import lombok.Data;

@Data
public class EventFilterDto {

    public String dateFromString;
    public String dateToString;
    public String plateNumber;
    public Long gateId;
}
