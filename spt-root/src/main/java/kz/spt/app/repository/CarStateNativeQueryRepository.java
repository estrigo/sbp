package kz.spt.app.repository;

import kz.spt.app.mapper.model.dto.CarStateExcelDtoMapper;
import kz.spt.app.model.dto.CarStateNativeQueryExcelDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * Здесь хранятся нативные методы для долгих запросов на выгрузку данных
 */
@Repository
@RequiredArgsConstructor
public class CarStateNativeQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<CarStateNativeQueryExcelDto> findAllWithFiltersForExcelReport(String carNumber,
                                                                              String inTimestamp,
                                                                              String outTimestamp,
                                                                              String inGateId,
                                                                              String outGateId,
                                                                              String paymentAmount) {

        StringBuilder mainQuery = new StringBuilder("SELECT ")
                .append(" cs.car_number, cs.in_timestamp, cs.out_timestamp, g_in.name as in_gate_name, g_out.name as out_gate_name ")
                .append(" FROM car_state as cs ")
                .append(" left join gate as g_in on (cs.in_gate = g_in.id) ")
                .append(" left join gate as g_out on (cs.out_gate = g_out.id) ")
                .append(" left join payments as p on (cs.payment_id = p.id)") // на данный момент в запросе на выгрузку нигде нет значения платежа
                .append(" where true");

        String dateCondition = " and (cs.in_timestamp between '" + inTimestamp + "' and '" + outTimestamp + "')";
        mainQuery.append(dateCondition);

        if (!Objects.equals(carNumber, EMPTY)) {
            String carNumberCondition = " and cs.car_number like '%" + carNumber + "%'";
            mainQuery.append(carNumberCondition);
        }

        if (!Objects.equals(inGateId, EMPTY)) {
            String inGateCondition = " and g_in.id = " + inGateId;
            mainQuery.append(inGateCondition);
        }

        if (!Objects.equals(outGateId, EMPTY)) {
            String outGateCondition = " and g_out.id = " + outGateId;
            mainQuery.append(outGateCondition);
        }

        if (!Objects.equals(paymentAmount, EMPTY)) {
            String paymentCondition = " and p.amount = " + paymentAmount;
            mainQuery.append(paymentCondition);
        }

        String sortingMode = " order by cs.in_timestamp desc, cs.out_timestamp desc ";
        String limit = " limit 1000000 ";
        mainQuery.append(sortingMode).append(limit);

        return jdbcTemplate.query(mainQuery.toString(), new CarStateExcelDtoMapper());
    }
}
