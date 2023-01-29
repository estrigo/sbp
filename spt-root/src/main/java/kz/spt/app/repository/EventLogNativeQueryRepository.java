package kz.spt.app.repository;


import kz.spt.app.mapper.model.dto.EventLogExcelDtoMapper;
import kz.spt.app.model.dto.EventLogNativeQueryExcelDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Здесь хранятся нативные методы для долгих запросов на выгрузку данных
 */
@Repository
@RequiredArgsConstructor
public class EventLogNativeQueryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final static int ROWS_LIMIT = 1_000_000;

    public List<EventLogNativeQueryExcelDto> findAllWithFiltersForExcelReport(String plateNumber,
                                                                              String fromTimestamp,
                                                                              String toTimestamp,
                                                                              String eventType) {

        StringBuilder mainQuery = new StringBuilder("SELECT ")
                .append(" el.plate_number, el.created, el.description, el.status_type ")
                .append(" FROM event_log as el ")
                .append(" WHERE true");

        String dateCondition = " AND (el.created between '" + fromTimestamp + "' and '" + toTimestamp + "')";
        mainQuery.append(dateCondition);

        if (!Objects.equals(plateNumber, EMPTY)) {
            String plateNumberCondition = " AND el.plate_number like '%" + plateNumber + "%'";
            mainQuery.append(plateNumberCondition);
        }

        if (!Objects.equals(eventType, EMPTY)) {
            String eventTypeCondition = " AND el.event_type like '" + eventType + "'";
            mainQuery.append(eventTypeCondition);
        }

        String sortingMode = " ORDER BY el.created desc ";
        mainQuery.append(sortingMode);

        jdbcTemplate.setMaxRows(ROWS_LIMIT);
        List<EventLogNativeQueryExcelDto> query = jdbcTemplate.query(mainQuery.toString(), new EventLogExcelDtoMapper());
        return query;
    }
}
