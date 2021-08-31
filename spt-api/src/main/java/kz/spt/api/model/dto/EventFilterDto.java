package kz.spt.api.model.dto;

import lombok.Data;

@Data
public class EventFilterDto {

    public String dateFromString;
    public String dateToString;
    public String plateNumber;
    public String description;
    public Long gateId;
    public String eventType;
}
