package kz.spt.app.converter.model.dto;

import kz.spt.app.model.dto.EventLogNativeQueryExcelDto;
import kz.spt.lib.model.dto.EventLogExcelDto;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class EventLogExcelDtoConverter implements Function<EventLogNativeQueryExcelDto, EventLogExcelDto> {

    @Override
    public EventLogExcelDto apply(@NonNull EventLogNativeQueryExcelDto fromDatabase) {
        return new EventLogExcelDto()
                .setPlateNumber(fromDatabase.plateNumber)
                .setCreated(fromDatabase.getCreated().toString())
                .setDescription(fromDatabase.description)
                .setStatus(fromDatabase.status)
                .setGate(""); // на данный момент значение gateId лежит внутри jsona в одном из столбцов таблицы EventLog. было принято решение не извлекать его
    }
}
