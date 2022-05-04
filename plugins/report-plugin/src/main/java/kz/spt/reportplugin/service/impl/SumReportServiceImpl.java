package kz.spt.reportplugin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.spt.lib.bootstrap.datatable.Column;
import kz.spt.lib.bootstrap.datatable.Order;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.PluginService;
import kz.spt.reportplugin.datatable.JournalReportDtoComparators;
import kz.spt.reportplugin.datatable.SumReportDtoComparators;
import kz.spt.reportplugin.dto.JournalReportDto;
import kz.spt.reportplugin.dto.SumReportDto;
import kz.spt.reportplugin.dto.filter.FilterJournalReportDto;
import kz.spt.reportplugin.dto.filter.FilterReportDto;
import kz.spt.reportplugin.dto.filter.FilterSumReportDto;
import kz.spt.reportplugin.service.ReportService;
import kz.spt.reportplugin.service.RootServicesGetterService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jvnet.hk2.annotations.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SumReportServiceImpl implements ReportService<SumReportDto> {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Comparator<SumReportDto> EMPTY_COMPARATOR = (e1, e2) -> 0;
    private ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    @Override
    public List<SumReportDto> list(FilterReportDto filterReportDto) {
        var filter = (FilterSumReportDto) filterReportDto;
        List<SumReportDto> sumResult = countSum(filter);

        return sumResult;
    }

    public List<SumReportDto> countSum(FilterSumReportDto filterSumReportDto){

        List<SumReportDto> results = new ArrayList<>();

        String query = "select DATE_FORMAT(cs.out_timestamp, '%Y.%m.%d') as datetime, " +
                "       count(cs.id) as count, " +
                "       count(cs.amount) as paymentsCount, " +
                "       count(cs.whitelist_json) as whitelistsCount, " +
                "       count(cs.abonoment_json) as abonementCount, " +
                "       sum(payments.kaspiSumma) as kaspiSumma, " +
                "       sum(payments.yurtaSumma) as yurtaSumma, " +
                "       sum(payments.totalSumma) as totalSum " +
                "from car_state cs " +
                "    left outer join ( " +
                "        select p.car_state_id as car_state_id, " +
                "        sum(p.amount) as totalSumma, " +
                "        sum(if(pp.client_id = 'kaspi', p.amount, 0)) as kaspiSumma, " +
                "        sum(if(pp.client_id = 'yurta', p.amount, 0)) as yurtaSumma " +
                "        from payments p " +
                "            inner join payment_provider pp on p.provider_id = pp.id " +
                "        group by p.car_state_id " +
                "    ) as payments on payments.car_state_id = cs.id " +
                "where cs.out_timestamp is not null " +
                "and cs.out_timestamp between :dateFrom and :dateTo " +
                "group by DATE_FORMAT(cs.out_timestamp, '%Y.%m.%d') " +
                "order by DATE_FORMAT(cs.out_timestamp, '%Y.%m.%d') desc";

        List<Object[]> objects =  entityManager.createNativeQuery(query)
                .setParameter("dateFrom", filterSumReportDto.getDateFrom())
                .setParameter("dateTo", filterSumReportDto.getDateTo())
                .getResultList();

        SimpleDateFormat checkFormat = new SimpleDateFormat("yyyy.MM.dd");
        SimpleDateFormat correctFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

        for(Object[] object: objects){
            String dateTime = (String) object[0];
            StringBuilder dateTimeString = new StringBuilder("");
            if(dateTime.equals(checkFormat.format(filterSumReportDto.getDateFrom()))){
                dateTimeString.append(correctFormat.format(filterSumReportDto.getDateFrom()));
            } else {
                dateTimeString.append(dateTime.substring(8));
                dateTimeString.append(".");
                dateTimeString.append(dateTime, 5, 7);
                dateTimeString.append(".");
                dateTimeString.append(dateTime, 2, 4);
                dateTimeString.append(" 00:00");
            }
            dateTimeString.append("\n");
            if(dateTime.equals(checkFormat.format(filterSumReportDto.getDateTo()))){
                dateTimeString.append(correctFormat.format(filterSumReportDto.getDateTo()));
            } else {
                dateTimeString.append(dateTime.substring(8));
                dateTimeString.append(".");
                dateTimeString.append(dateTime, 5, 7);
                dateTimeString.append(".");
                dateTimeString.append(dateTime, 2, 4);
                dateTimeString.append(" 23:59");
            }

            SumReportDto dto = SumReportDto.builder()
                    .dateTime(dateTimeString.toString())
                    .count(((BigInteger) object[1]).intValue())
                    .paymentsCount(((BigInteger) object[2]).intValue())
                    .whitelistsCount(((BigInteger) object[3]).intValue())
                    .abonementsCount(((BigInteger) object[4]).intValue())
                    .kaspiSum(((BigDecimal) object[5]))
                    .yurtaSum(((BigDecimal) object[6]))
                    .totalSum(((BigDecimal) object[7]))
                    //.freeCount(((BigInteger) object[8]).intValue())
                    .build();
            results.add(dto);
        }
        return results;
    }

    @Override
    public Page<SumReportDto> page(PagingRequest pagingRequest) {

        var all = list(convert(pagingRequest));

        var filtered = all.stream()
                .filter(filterPage(pagingRequest))
                .sorted(sortPage(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = all.stream().filter(filterPage(pagingRequest)).count();

        Page<SumReportDto> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    @Override
    public Predicate<SumReportDto> filterPage(PagingRequest pagingRequest) {
        return result -> true;
    }

    @Override
    public Comparator<SumReportDto> sortPage(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder().get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns().get(columnIndex);

            Comparator<SumReportDto> comparator = SumReportDtoComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }

    private FilterSumReportDto convert(PagingRequest pagingRequest) {
        return pagingRequest.convertTo(FilterSumReportDto.builder().build());
    }
}
