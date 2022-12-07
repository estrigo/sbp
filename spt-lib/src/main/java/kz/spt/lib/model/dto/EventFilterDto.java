package kz.spt.lib.model.dto;

import kz.spt.lib.model.EventLog;
import lombok.Data;

@Data
public class EventFilterDto {

    public String dateFromString;
    public String dateToString;
    public String plateNumber;
    public Long gateId;
    public EventLog.EventType eventType;
}
