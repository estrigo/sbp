package kz.spt.app.mapper.model.dto;

import kz.spt.app.model.dto.EventLogNativeQueryExcelDto;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EventLogExcelDtoMapper implements RowMapper<EventLogNativeQueryExcelDto> {

    private final static String PLATE_NUMBER = "plate_number";
    private final static String CREATED = "created";
    private final static String DESCRIPTION = "description";
    private final static String STATUS = "status_type";

    @Override
    public EventLogNativeQueryExcelDto mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new EventLogNativeQueryExcelDto()
                .setPlateNumber(resultSet.getString(PLATE_NUMBER))
                .setCreated(resultSet.getDate(CREATED))
                .setDescription(resultSet.getString(DESCRIPTION))
                .setStatus(resultSet.getString(STATUS));
    }
}