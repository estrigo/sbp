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
import kz.spt.reportplugin.dto.SumReportListDto;
import kz.spt.reportplugin.dto.filter.FilterJournalReportDto;
import kz.spt.reportplugin.dto.filter.FilterReportDto;
import kz.spt.reportplugin.dto.filter.FilterSumReportDto;
import kz.spt.reportplugin.service.ReportService;
import kz.spt.reportplugin.service.RootServicesGetterService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.jvnet.hk2.annotations.Service;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
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
        if("detailed".equals(filter.getType())){
            return listDetailed(filter);
        }
        return countSum(filter);
    }

    public List<SumReportDto> countSum(FilterSumReportDto filterSumReportDto){

        Calendar dateFrom = Calendar.getInstance();
        dateFrom.setTime(filterSumReportDto.getDateFrom());
        dateFrom.add(Calendar.MINUTE, -6);

        Calendar dateTo = Calendar.getInstance();
        dateTo.setTime(filterSumReportDto.getDateTo());
        dateTo.add(Calendar.MINUTE, 6);

        //----------------------------------------------- In cars count --------------
        List<Object[]> inCars = entityManager
                .createNativeQuery("select DATE_FORMAT(date_add(cs.in_timestamp, INTERVAL 6 hour), '%Y.%m.%d') as datetime, count(cs.id) " +
        "                from car_state cs " +
        "        where cs.in_timestamp between :dateFrom and :dateTo " +
        "        group by DATE_FORMAT(date_add(cs.in_timestamp, INTERVAL 6 hour), '%Y.%m.%d') " +
        "        order by DATE_FORMAT(date_add(cs.in_timestamp, INTERVAL 6 hour), '%Y.%m.%d') desc")
                .setParameter("dateFrom", filterSumReportDto.getDateFrom())
                .setParameter("dateTo", filterSumReportDto.getDateTo())
                .getResultList();
        Map<String, Object> inCarsMap = new HashMap<>(inCars.size());
        for(Object[] inCar: inCars){
            inCarsMap.put((String) inCar[0], inCar[1]);
        }

        //------------------------------------------------Out cars count--------------


        List<Object[]> providers = entityManager.createNativeQuery("select pp.client_id, pp.name, pp.cashless_payment from payment_provider pp where name not like ('%test%')").getResultList();

        Boolean hasCashPayment = false;
        for(Object[] provider: providers){
            Boolean cashless = (Boolean) provider[2];
            if(cashless != null && !cashless){
                hasCashPayment = true;
            }
        }

        String headerQuery = "select DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%Y.%m.%d') as datetime, " +
        "       count(cs.id) as count, " +
        "       sum(if(payments.totalSumma>0 and paidEvent.car_state_id is not null and cs.out_gate is not null,1,0)) as paymentsCount, " +
        "       sum(if(whitelistEvent.car_state_id is not null and cs.out_gate is not null, 1, 0)) as whitelistsCount, " +
        "       sum(if(abonementEvent.car_state_id is not null and cs.out_gate is not null, 1, 0)) as abonementEvent," +
        "       sum(if(freeMinutes.car_state_id is not null and cs.out_gate is not null,1,0)) as min15Free," +
        "       sum(if(debt.car_state_id is not null and cs.out_gate is not null,1,0)) as debt," +
        "       sum(if((payments.totalSumma is null or payments.totalSumma=0) and paidEvent.car_state_id is not null and cs.out_gate is not null,1,0)) as fromBalance," +
        "       sum(if(free.car_state_id is not null and freeMinutes.car_state_id is null and whitelistEvent.car_state_id is null and paidEvent.car_state_id is null and debt.car_state_id is null and abonementEvent.car_state_id is null and cs.out_gate is not null,1,0)) as free," +
        "       sum(if(cs.out_gate is null,1,0)) as autoClosed ";

        if(hasCashPayment){
            headerQuery = headerQuery +
           "        ,sum(payments.cardsSumma) as cardsSumma " +
           "        ,sum(payments.cashSumma) as cashSumma ";
        }
        for(Object[] provider: providers){
            headerQuery = headerQuery +
           "       ,sum(payments." + provider[0] + "Summa) as " + provider[0] + "Summa ";
        }

        headerQuery = headerQuery +
        "       ,sum(payments.totalSumma) as totalSum ";

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
        " left outer join ( " +
        "            select cs.id as car_state_id, count(l.id) " +
        "            from event_log l" +
        "                inner join ( " +
        "                    select cs.id, cs.out_timestamp, cs.car_number " +
        "                    from car_state cs " +
        "                    where cs.out_timestamp between :dateFrom and :dateTo " +
        "                ) cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 6 second) and date_add(l.created, INTERVAL 6 second) " +
        "            where l.object_class = 'Gate' " +
        "            and l.created between :dateFromException and :dateToException " +
        "            and l.event_type = 'WHITELIST_OUT' " +
        "            group by cs.id " +
        "       ) as whitelistEvent on whitelistEvent.car_state_id = cs.id" + 
        "        left outer join ( " +
        "            select cs.id as car_state_id, count(l.id) " +
        "            from event_log l " +
        "            inner join ( " +
        "                select cs.id, cs.out_timestamp, cs.car_number " +
        "                from car_state cs " +
        "                where cs.out_timestamp between :dateFrom and :dateTo " +
        "            ) cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 6 second) and date_add(l.created, INTERVAL 6 second) " +
        "            where l.object_class = 'Gate' " +
        "              and l.created between :dateFromException and :dateToException " +
        "              and l.event_type = 'ABONEMENT_PASS' " +
        "            group by cs.id " +
        "        ) as abonementEvent on abonementEvent.car_state_id = cs.id" +
        "        left outer join ( " +
        "            select cs.id as car_state_id, count(l.id) " +
        "            from event_log l " +
        "                     inner join ( " +
        "                select cs.id, cs.out_timestamp, cs.car_number " +
        "                from car_state cs " +
        "                where cs.out_timestamp between :dateFrom and :dateTo " +
        "            ) cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 6 second) and date_add(l.created, INTERVAL 6 second) " +
        "            where l.object_class = 'Gate' " +
        "              and l.created between :dateFromException and :dateToException " +
        "              and l.event_type = 'PAID_PASS' " +
        "            group by cs.id " +
        "       ) as paidEvent on paidEvent.car_state_id = cs.id" +
        "       left outer join ( " +
        "            select cs.id as car_state_id, count(l.id) " +
        "            from event_log l " +
        "                     inner join ( " +
        "                select cs.id, cs.out_timestamp, cs.car_number " +
        "                from car_state cs " +
        "                where cs.out_timestamp between :dateFrom and :dateTo " +
        "            ) cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 6 second) and date_add(l.created, INTERVAL 6 second) " +
        "            where l.object_class = 'Gate' " +
        "              and l.created between :dateFromException and :dateToException " +
        "              and l.event_type = 'FIFTEEN_FREE' " +
        "            group by cs.id " +
        "        ) as freeMinutes on freeMinutes.car_state_id = cs.id" +
        "       left outer join (  " +
        "            select cs.id as car_state_id, count(l.id)  " +
        "            from event_log l  " +
        "                     inner join (  " +
        "                select cs.id, cs.out_timestamp, cs.car_number  " +
        "                from car_state cs  " +
        "                where cs.out_timestamp between :dateFrom and :dateTo  " +
        "            ) cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 6 second) and date_add(l.created, INTERVAL 6 second)  " +
        "            where l.object_class = 'Gate'  " +
        "              and l.created between :dateFromException and :dateToException  " +
        "              and l.event_type = 'DEBT_OUT' " +
        "            group by cs.id  " +
        "        ) as debt on debt.car_state_id = cs.id" +
        "       left outer join (  " +
        "            select cs.id as car_state_id, count(l.id)  " +
        "            from event_log l  " +
        "                     inner join (  " +
        "                select cs.id, cs.out_timestamp, cs.car_number  " +
        "                from car_state cs  " +
        "                where cs.out_timestamp between :dateFrom and :dateTo  " +
        "            ) cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 6 second) and date_add(l.created, INTERVAL 6 second)  " +
        "            where l.object_class = 'Gate'  " +
        "              and l.created between :dateFromException and :dateToException  " +
        "              and l.event_type = 'FREE_PASS'  " +
        "            group by cs.id  " +
        "        ) as free on free.car_state_id = cs.id " +
        " where cs.out_timestamp is not null " +
        " and cs.out_timestamp between :dateFrom and :dateTo " +
        " group by DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%Y.%m.%d') " +
        " order by DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%Y.%m.%d') desc";

        List<Object[]> objects = entityManager.createNativeQuery(headerQuery + bodyQuery)
                .setParameter("dateFrom", filterSumReportDto.getDateFrom())
                .setParameter("dateTo", filterSumReportDto.getDateTo())
                .setParameter("dateFromException", dateFrom.getTime())
                .setParameter("dateToException", dateTo.getTime())
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

        Map<String, String> fieldsMap = new HashMap<>(15);
        fieldsMap.put("dateTime", bundle.getString("report.dateTime"));
        fieldsMap.put("records", bundle.getString("report.outDateCount"));
        fieldsMap.put("inDateCount", bundle.getString("report.inDateCount"));
        fieldsMap.put("paymentRecords", bundle.getString("report.paymentRecords"));
        fieldsMap.put("whitelistRecords", bundle.getString("report.whitelistRecords"));
        fieldsMap.put("abonementRecords", bundle.getString("report.abonementRecords"));
        fieldsMap.put("freeMinuteRecords", bundle.getString("report.freeMinuteRecords"));
        fieldsMap.put("debtRecords", bundle.getString("report.debtRecords"));
        fieldsMap.put("fromBalanceRecords", bundle.getString("report.fromBalanceRecords"));
        fieldsMap.put("freeRecords", bundle.getString("report.freeRecords"));
        fieldsMap.put("autoClosedRecords", bundle.getString("report.autoClosedRecords"));

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
            dateTimeString.append(" ");
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
            values.put("date", dateTime);
            values.put("dateTime", dateTimeString);
            values.put("records", object[it++]);
            values.put("paymentRecords", object[it++]);
            values.put("whitelistRecords", object[it++]);
            values.put("abonementRecords", object[it++]);
            values.put("freeMinuteRecords", object[it++]);
            values.put("debtRecords", object[it++]);
            values.put("fromBalanceRecords", object[it++]);
            values.put("freeRecords", object[it++]);
            values.put("autoClosedRecords", object[it++]);

            if(hasCashPayment){
                values.put("bankCardSum", object[it++]);
                values.put("cashSum", object[it++]);
            }
            for(Object[] provider: providers){
                values.put((String) provider[0], object[it++]);
            }
            values.put("totalSum", object[it++]);
            values.put("inDateCount", inCarsMap.containsKey(dateTime) ? inCarsMap.get(dateTime) : 0);
            sumReportDto.setResults(values);
            results.add(sumReportDto);
        }

        return results;
    }

    public List<SumReportDto> listDetailed(FilterSumReportDto filterSumReportDto) throws ParseException {

        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        Date date = format.parse(filterSumReportDto.getDate());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.HOUR_OF_DAY, -6);

        Calendar dateFromCalendar = Calendar.getInstance();
        dateFromCalendar.setTime(calendar.getTime());
        if(calendar.getTime().before(filterSumReportDto.getDateFrom())){
            dateFromCalendar.setTime(filterSumReportDto.getDateFrom());
        }
        Date dateFrom = dateFromCalendar.getTime();
        dateFromCalendar.add(Calendar.MINUTE, -6);
        Date dateFromException = dateFromCalendar.getTime();

        calendar.add(Calendar.DATE, 1);

        Calendar dateToCalendar = Calendar.getInstance();
        dateToCalendar.setTime(calendar.getTime());
        if(calendar.getTime().after(filterSumReportDto.getDateTo())){
            dateToCalendar.setTime(filterSumReportDto.getDateTo());
        }
        Date dateTo = dateToCalendar.getTime();
        dateToCalendar.add(Calendar.MINUTE, 6);
        Date dateToException = dateToCalendar.getTime();

        String commonPart = " select distinct cs.car_number, DATE_FORMAT(date_add(cs.in_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as inDate, DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as outDate, inGate.name as gateIn, outGate.name as gateOut " +
                " from event_log l " +
                " inner join ( " +
                "    select cs.id, cs.out_timestamp, cs.car_number, cs.in_timestamp, cs.out_gate, cs.in_gate " +
                "    from car_state cs " +
                "    where cs.out_timestamp between :dateFrom and :dateTo " +
                " ) cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 6 second) and date_add(l.created, INTERVAL 6 second) " +
                " left outer join " +
                "     gate inGate on inGate.id = cs.in_gate " +
                " left outer join " +
                "     gate outGate on outGate.id = cs.out_gate " +
                " where l.object_class = 'Gate' " +
                "  and l.created between :dateFromException and :dateToException ";
        String queryText = null;

        switch (filterSumReportDto.getEventType()) {
            case  ("whitelistRecords"):
                queryText = commonPart + "  and l.event_type = 'WHITELIST_OUT'";
                break;
            case ("abonementRecords"):
                queryText = commonPart + "  and l.event_type = 'ABONEMENT_PASS'";
                break;
            case ("freeMinuteRecords"):
                queryText = commonPart + "  and l.event_type = 'FIFTEEN_FREE'";
                break;
            case ("debtRecords"):
                queryText = commonPart + "  and l.event_type = 'DEBT_OUT'";
                break;
            case ("freeRecords"):
                queryText = commonPart + "  and l.event_type = 'FREE_PASS'";
                break;
            case ("paymentRecords"):
                queryText = " select distinct cs.car_number, DATE_FORMAT(date_add(cs.in_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as inDate, DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as outDate, inGate.name as gateIn, outGate.name as gateOut " +
                        " from event_log l " +
                        " inner join ( " +
                        "    select cs.id, cs.out_timestamp, cs.car_number, cs.in_timestamp, cs.out_gate, cs.in_gate " +
                        "    from car_state cs " +
                        "    where cs.out_timestamp between :dateFrom and :dateTo " +
                        " ) cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 6 second) and date_add(l.created, INTERVAL 6 second) " +
                        " left outer join " +
                        "     gate inGate on inGate.id = cs.in_gate " +
                        " left outer join " +
                        "     gate outGate on outGate.id = cs.out_gate" +
                        " left outer join ( " +
                        "    select p.car_state_id as car_state_id, " +
                        "           sum(p.amount) as totalSumma " +
                        "    from payments p " +
                        "             inner join car_state cs on cs.id = p.car_state_id and cs.out_timestamp between :dateFrom and :dateTo " +
                        "             inner join payment_provider pp on p.provider_id = pp.id " +
                        "    group by p.car_state_id " +
                        ") as payments on payments.car_state_id = cs.id " +
                        " where l.object_class = 'Gate' " +
                        " and l.created between :dateFromException and :dateToException  " +
                        " and l.event_type = 'PAID_PASS'" +
                        " and payments.totalSumma > 0";
                break;
            case ("fromBalanceRecords"):
                queryText = " select distinct cs.car_number, DATE_FORMAT(date_add(cs.in_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as inDate, DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as outDate, inGate.name as gateIn, outGate.name as gateOut " +
                        " from event_log l " +
                        " inner join ( " +
                        "    select cs.id, cs.out_timestamp, cs.car_number, cs.in_timestamp, cs.out_gate, cs.in_gate " +
                        "    from car_state cs " +
                        "    where cs.out_timestamp between :dateFrom and :dateTo " +
                        " ) cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 6 second) and date_add(l.created, INTERVAL 6 second) " +
                        " left outer join " +
                        "     gate inGate on inGate.id = cs.in_gate " +
                        " left outer join " +
                        "     gate outGate on outGate.id = cs.out_gate" +
                        " left outer join ( " +
                        "    select p.car_state_id as car_state_id, " +
                        "           sum(p.amount) as totalSumma " +
                        "    from payments p " +
                        "             inner join car_state cs on cs.id = p.car_state_id and cs.out_timestamp between :dateFrom and :dateTo " +
                        "             inner join payment_provider pp on p.provider_id = pp.id " +
                        "    group by p.car_state_id " +
                        ") as payments on payments.car_state_id = cs.id " +
                        " where l.object_class = 'Gate' " +
                        " and l.created between :dateFromException and :dateToException  " +
                        " and l.event_type = 'PAID_PASS'" +
                        " and (payments.totalSumma is null or payments.totalSumma = 0)";
                break;
            case ("autoClosedRecords"):
                queryText = "select distinct cs.car_number, DATE_FORMAT(date_add(cs.in_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as inDate, DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as outDate, inGate.name as gateIn, outGate.name as gateOut " +
                        " from car_state cs " +
                        " left outer join " +
                        "     gate inGate on inGate.id = cs.in_gate " +
                        " left outer join " +
                        "     gate outGate on outGate.id = cs.out_gate " +
                        " where cs.out_timestamp between :dateFrom and :dateTo " +
                        " and cs.out_gate is null";
                break;
            default:
                queryText = null;
                break;
        }

        log.info(queryText);
        log.info("dateFrom: " + dateFrom);
        log.info("dateFrom: " + dateTo);
        log.info("dateFromException: " + dateFromException);
        log.info("dateToException: " + dateToException);

        Query query = entityManager
                .createNativeQuery(queryText)
                .setParameter("dateFrom", dateFrom)
                .setParameter("dateTo", dateTo);

        if(!"autoClosedRecords".equals(filterSumReportDto.getEventType())){
            query.setParameter("dateFromException", dateFromException).setParameter("dateToException", dateToException);
        }

        List<Object[]> queryResult = query.getResultList();

        List<SumReportDto> listResult = new ArrayList<>(queryResult.size());

        for(Object[] object: queryResult){
            int it = 0;
            SumReportDto sumReportDto = new SumReportDto();
            SumReportListDto dto = new SumReportListDto();
            dto.setPlateNumber((String)object[it++]);
            dto.setFormattedInDate((String)object[it++]);
            dto.setFormattedOutDate((String)object[it++]);
            dto.setInPlace((String)object[it++]);
            dto.setOutPlace((String)object[it++]);
            sumReportDto.setListResult(dto);
            listResult.add(sumReportDto);
        }
        return listResult;
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
