package kz.spt.app.mapper.model.dto;

import kz.spt.app.model.dto.CarStateNativeQueryExcelDto;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CarStateExcelDtoMapper implements RowMapper<CarStateNativeQueryExcelDto> {

    private final static String CAR_NUMBER_COLUMN_NAME = "car_number";
    private final static String IN_TIMESTAMP_COLUMN_NAME = "in_timestamp";
    private final static String OUT_TIMESTAMP_COLUMN_NAME = "out_timestamp";
    private final static String IN_GATE_COLUMN_NAME = "in_gate_name";
    private final static String OUT_GATE_COLUMN_NAME = "out_gate_name";

    @Override
    public CarStateNativeQueryExcelDto mapRow(ResultSet resultSet, int i) throws SQLException {
        return new CarStateNativeQueryExcelDto()
                .setCarNumber(resultSet.getString(CAR_NUMBER_COLUMN_NAME))
                .setInTimestamp(resultSet.getTimestamp(IN_TIMESTAMP_COLUMN_NAME))
                .setOutTimestamp(resultSet.getDate(OUT_TIMESTAMP_COLUMN_NAME))
                .setInGate(resultSet.getString(IN_GATE_COLUMN_NAME))
                .setOutGate(resultSet.getString(OUT_GATE_COLUMN_NAME));
    }
}
