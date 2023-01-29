package kz.spt.app.converter.model.dto;

import kz.spt.app.model.dto.CarStateNativeQueryExcelDto;
import kz.spt.lib.model.dto.EventLogExcelDto;

import java.util.function.Function;

public class EventLogExcelDtoConverter implements Function<EventLogNativeQueryExcelDto, EventLogExcelDto> {
    @Override
    public EventLogExcelDto apply(EventLogNativeQueryExcelDto eventLogNativeQueryExcelDto) {
        return null;
    }
}
