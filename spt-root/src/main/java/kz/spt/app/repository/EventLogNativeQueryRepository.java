package kz.spt.app.repository;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventLogNativeQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    private final static String QUERY =
            "select el.plate_number, el.created, el.description, el.status_type from event_log as el";
    private final static int ROWS_LIMIT = 1_000_000;

    public List<EventLogNativeQueryExcelDto> findAllWithFiltersForExcelReport() {
        jdbcTemplate.setMaxRows(ROWS_LIMIT);
        List<EventLogNativeQueryExcelDto> query = jdbcTemplate.query(QUERY, new EventLogExcelReportDtoRowMapper());
        return query;
    }
}
