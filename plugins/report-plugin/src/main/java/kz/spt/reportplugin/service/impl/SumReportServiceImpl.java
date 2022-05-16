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
import org.springframework.context.i18n.LocaleContextHolder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
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

        List<Object[]> providers = entityManager.createNativeQuery("select pp.client_id, pp.name, pp.cashless_payment from payment_provider pp where name not like ('%test%')").getResultList();

        Boolean hasCashPayment = false;
        for(Object[] provider: providers){
            Boolean cashless = (Boolean) provider[2];
            if(cashless != null && !cashless){
                hasCashPayment = true;
            }
        }

        ArrayList<String> fields = new ArrayList<>(10);

        String headerQuery = "select DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%Y.%m.%d') as datetime, " +
                "       count(cs.id) as count, " +
                "       count(cs.amount) as paymentsCount, " +
                "       count(cs.whitelist_json) as whitelistsCount, " +
                "       count(cs.abonoment_json) as abonementCount";
        fields.add("dateTime");
        fields.add("records");
        fields.add("paymentRecords");
        fields.add("whitelistRecords");
        fields.add("abonementRecords");

        if(hasCashPayment){
            headerQuery = headerQuery +
                    "        ,sum(payments.cardsSumma) as cardsSumma " +
                    "        ,sum(payments.cashSumma) as cashSumma ";
            fields.add("bankCardSum");
            fields.add("cashSum");
        }
        for(Object[] provider: providers){
            headerQuery = headerQuery +
                    "       ,sum(payments." + provider[0] + "Summa) as " + provider[0] + "Summa ";
            fields.add((String) provider[1]);
        }

        headerQuery = headerQuery +
                "       ,sum(payments.totalSumma) as totalSum ";
        fields.add("totalSum");

        String bodyQuery =
                "from car_state cs " +
                "    left outer join ( " +
                "        select p.car_state_id as car_state_id, " +
                "        sum(p.amount) as totalSumma ";

        if(hasCashPayment){
            bodyQuery = bodyQuery +
                "        ,sum(if(pp.cashless_payment = false and p.ikkm = true, p.amount, 0)) as cardsSumma " +
                "        ,sum(if(pp.cashless_payment = false and (p.ikkm is null or p.ikkm = false), p.amount, 0)) as cashSumma ";

        }
        for(Object[] provider: providers){
            bodyQuery = bodyQuery +
                    "        ,sum(if(pp.provider = '" + provider[0] + "', p.amount, 0)) as " + provider[0] + "Summa ";
        }

        bodyQuery = bodyQuery +
        " " +
        "       from payments p " +
        "            inner join payment_provider pp on p.provider_id = pp.id " +
        "        group by p.car_state_id " +
        "    ) as payments on payments.car_state_id = cs.id " +
        "where cs.out_timestamp is not null " +
        "and cs.out_timestamp between :dateFrom and :dateTo " +
        "group by DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%Y.%m.%d') " +
        "order by DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%Y.%m.%d') desc";

        List<Object[]> objects = entityManager.createNativeQuery(headerQuery + bodyQuery)
                .setParameter("dateFrom", filterSumReportDto.getDateFrom())
                .setParameter("dateTo", filterSumReportDto.getDateTo())
                .getResultList();

        SimpleDateFormat checkFormat = new SimpleDateFormat("yyyy.MM.dd");
        SimpleDateFormat correctFormat = new SimpleDateFormat("dd.MM.yy HH:mm");

        List<SumReportDto> results = new ArrayList<>(objects.size());

        Locale locale = LocaleContextHolder.getLocale();
        String language = "en";
        if (locale.toString().equals("ru")) {
            language = "ru";
        }
        ResourceBundle bundle = ResourceBundle.getBundle("report-plugin", Locale.forLanguageTag(language));

        Map<String, String> fieldsMap = new HashMap<>(10);
        fieldsMap.put("dateTime", bundle.getString("report.dateTime"));
        fieldsMap.put("records", bundle.getString("report.records"));
        fieldsMap.put("paymentRecords", bundle.getString("report.paymentRecords"));
        fieldsMap.put("whitelistRecords", bundle.getString("report.whitelistRecords"));
        fieldsMap.put("abonementRecords", bundle.getString("report.abonementRecords"));
        if(hasCashPayment){
            fieldsMap.put("bankCardSum", bundle.getString("report.bankCardSum"));
            fieldsMap.put("cashSum", bundle.getString("report.cashSum"));
        }
        for(Object[] provider: providers){
            fieldsMap.put((String) provider[0], (String) provider[1]);
        }
        fieldsMap.put("totalSum", bundle.getString("report.totalSum"));
        SumReportDto firstDto = new SumReportDto();
        firstDto.setFields(fieldsMap);
        results.add(firstDto);

        for(Object[] object: objects){
            int it = 0;
            SumReportDto sumReportDto = new SumReportDto();

            String dateTime = (String) object[it++];
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
            Map<String, Object> values = new HashMap<>(10);
            values.put("dateTime", dateTimeString);
            values.put("records", object[it++]);
            values.put("paymentRecords", object[it++]);
            values.put("whitelistRecords", object[it++]);
            values.put("abonementRecords", object[it++]);
            if(hasCashPayment){
                values.put("bankCardSum", object[it++]);
                values.put("cashSum", object[it++]);
            }
            for(Object[] provider: providers){
                values.put((String) provider[0], object[it++]);
            }
            values.put("totalSum", object[it++]);
            sumReportDto.setResults(values);
            results.add(sumReportDto);
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
