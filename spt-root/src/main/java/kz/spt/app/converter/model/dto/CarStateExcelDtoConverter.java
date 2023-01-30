package kz.spt.app.converter.model.dto;

import kz.spt.app.model.dto.CarStateNativeQueryExcelDto;
import kz.spt.app.utils.DateTimeUtil;
import kz.spt.lib.model.dto.CarStateExcelDto;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.function.Function;

@Component
public class CarStateExcelDtoConverter implements Function<CarStateNativeQueryExcelDto, CarStateExcelDto> {

    @Override
    public CarStateExcelDto apply(@NonNull CarStateNativeQueryExcelDto fromDatabase) {
        Date inTimestamp = fromDatabase.getInTimestamp();
        Date outTimestamp = fromDatabase.getOutTimestamp();
        String inTimestampString = inTimestamp == null ? new Date(0L).toString() : inTimestamp.toString();
        String outTimestampString = outTimestamp == null ? new Date().toString() : outTimestamp.toString();

        return new CarStateExcelDto()
                .setCarNumber(fromDatabase.getCarNumber())
                .setInTimestampString(inTimestampString)
                .setOutTimestampString(outTimestampString)
                .setInGate(fromDatabase.getInGate())
                .setOutGate(fromDatabase.getOutGate())
                .setDuration(DateTimeUtil.getFormattedDurationString(inTimestamp, outTimestamp))
                .setStatus(fromDatabase.getStatus());
    }
}
