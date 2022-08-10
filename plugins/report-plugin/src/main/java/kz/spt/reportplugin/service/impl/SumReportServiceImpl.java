package kz.spt.reportplugin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.spt.lib.bootstrap.datatable.Column;
import kz.spt.lib.bootstrap.datatable.Order;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.CurrentUser;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import kz.spt.reportplugin.ReportPlugin;
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
import org.springframework.security.core.context.SecurityContextHolder;

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
    private RootServicesGetterService rootServicesGetterService;
    private PluginService pluginService;

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

    @Override
    public Page<SumReportDto> list(PagingRequest pagingRequest, FilterReportDto filterReportDto) {
        return null;
    }

    public List<SumReportDto> countSum(FilterSumReportDto filterSumReportDto){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(filterSumReportDto.getDateFrom());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        filterSumReportDto.setDateFrom(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        filterSumReportDto.setDateTo(calendar.getTime());

        Calendar dateFromException = Calendar.getInstance();
        dateFromException.setTime(filterSumReportDto.getDateFrom());
        dateFromException.add(Calendar.MINUTE, -1);

        Calendar dateToException = Calendar.getInstance();
        dateToException.setTime(filterSumReportDto.getDateTo());
        dateToException.add(Calendar.MINUTE, 1);

        List<SumReportDto> results = new ArrayList<>(10);

        SumReportDto firstDto = new SumReportDto();
        if("fields".equals(filterSumReportDto.getEventType())){
            List<Object[]> providers = entityManager.createNativeQuery("select pp.client_id, pp.name, pp.cashless_payment from payment_provider pp where name not like ('%test%') and name not like ('%gateway%')").getResultList();

            Boolean hasCashPayment = false;
            for(Object[] provider: providers){
                Boolean cashless = (Boolean) provider[2];
                if(cashless != null && !cashless){
                    hasCashPayment = true;
                }
            }

            Locale locale = LocaleContextHolder.getLocale();
            String language = "en";
            if (locale.toString().equals("ru")) {
                language = "ru";
            }
            ResourceBundle bundle = ResourceBundle.getBundle("report-plugin", Locale.forLanguageTag(language));

            Map<String, String> fieldsMap = new HashMap<>(15);
            fieldsMap.put("dateTime", bundle.getString("report.dateTime"));
            fieldsMap.put("records", bundle.getString("report.outDateCount"));
            fieldsMap.put("paymentRecords", bundle.getString("report.paymentRecords"));
            fieldsMap.put("whitelistRecords", bundle.getString("report.whitelistRecords"));
            fieldsMap.put("abonementRecords", bundle.getString("report.abonementRecords"));
            fieldsMap.put("freeMinuteRecords", bundle.getString("report.freeMinuteRecords"));
            fieldsMap.put("debtRecords", bundle.getString("report.debtRecords"));
            fieldsMap.put("fromBalanceRecords", bundle.getString("report.fromBalanceRecords"));
            fieldsMap.put("freeRecords", bundle.getString("report.freeRecords"));
            fieldsMap.put("autoClosedRecords", bundle.getString("report.autoClosedRecords"));

            PluginRegister megaPluginRegister = getPluginService().getPluginRegister(StaticValues.megaPlugin);
            if(megaPluginRegister != null){
                fieldsMap.put("thirdPartyRecords", bundle.getString("report.thirdPartyRecords"));
            }

            if(hasCashPayment){
                fieldsMap.put("bankCardSum", bundle.getString("report.bankCardSum"));
                fieldsMap.put("cashSum", bundle.getString("report.cashSum"));
            }
            for(Object[] provider: providers){
                fieldsMap.put((String) provider[0], (String) provider[1]);
            }
            fieldsMap.put("totalSum", bundle.getString("report.totalSum"));

            firstDto.setResults(fieldsMap);
            results.add(firstDto);
        } else if("payments".equals(filterSumReportDto.getEventType())){
            List<Object[]> providers = entityManager.createNativeQuery("select pp.client_id, pp.name, pp.cashless_payment from payment_provider pp where name not like ('%test%')").getResultList();

            Boolean hasCashPayment = false;
            for(Object[] provider: providers){
                Boolean cashless = (Boolean) provider[2];
                if(cashless != null && !cashless){
                    hasCashPayment = true;
                }
            }

            String headerQuery = "select ";
            if(hasCashPayment){
                headerQuery = headerQuery + "sum(payments.cardsSumma) as cardsSumma, sum(payments.cashSumma) as cashSumma, ";
            }
            for(Object[] provider: providers){
                headerQuery = headerQuery +
                        "       sum(payments." + provider[0] + "Summa) as " + provider[0] + "Summa, ";
            }
            headerQuery = headerQuery +
                    "       sum(payments.totalSumma) as totalSum ";

            String bodyQuery = " from car_state cs " +
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
                    "        from payments p " +
                    "        inner join payment_provider pp on p.provider_id = pp.id " +
                    "        where p.out_date between :dateFrom and :dateTo or p.out_date is null" +
                    "        group by p.car_state_id " +
                    "    ) as payments on payments.car_state_id = cs.id " +
                    " where cs.out_timestamp between :dateFrom and :dateTo ";

            List<Object[]> objects = entityManager.createNativeQuery(headerQuery + bodyQuery)
                    .setParameter("dateFrom", filterSumReportDto.getDateFrom())
                    .setParameter("dateTo", filterSumReportDto.getDateTo())
                    .getResultList();

            for(Object[] object: objects){
                int it = 0;
                SumReportDto sumReportDto = new SumReportDto();

                Map<String, String> values = new HashMap<>(10);

                if(hasCashPayment){
                    values.put("bankCardSum", String.valueOf(object[it++]));
                    values.put("cashSum", String.valueOf(object[it++]));
                }
                for(Object[] provider: providers){
                    values.put((String) provider[0], String.valueOf(object[it++]));
                }
                values.put("totalSum", String.valueOf(object[it++]));
                sumReportDto.setResults(values);
                results.add(sumReportDto);
            }
        } else {
            String queryString = null;
            if("records".equals(filterSumReportDto.getEventType())){

                String username = "undefined";
                if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                    CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    if (currentUser != null) {
                        username = currentUser.getUsername();
                    }
                }
                log.info("Sum Report called by user: " + username + " from date: " + filterSumReportDto.getDateFrom()+ " to date: " + filterSumReportDto.getDateTo());

                queryString = "select count(cs.id) as count " +
                        "from car_state cs " +
                        "where cs.out_timestamp between :dateFrom and :dateTo";
            } else if("paymentRecords".equals(filterSumReportDto.getEventType())){
                queryString = "select count(distinct cs.car_state_id) " +
                        "from ( " +
                        "         select l.created, l.plate_number " +
                        "         from event_log l " +
                        "         where l.object_class = 'Gate' " +
                        "           and l.created between :dateFromException and :dateToException " +
                        "           and l.event_type = 'PAID_PASS' " +
                        ") l " +
                        "inner join ( " +
                        "    select p.car_state_id as car_state_id, cs.out_timestamp, cs.car_number, sum(p.amount) as totalSumma " +
                        "    from payments p " +
                        "             inner join car_state cs on cs.id = p.car_state_id " +
                        "    where (p.out_date between :dateFrom and :dateTo or p.out_date is null) " +
                        "      and cs.out_timestamp between :dateFrom and :dateTo " +
                        "      and cs.out_gate is not null " +
                        "    group by p.car_state_id " +
                        "    having totalSumma > 0 " +
                        ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second) ";
            } else if("whitelistRecords".equals(filterSumReportDto.getEventType())){
                queryString = "select count(distinct cs.car_state_id) " +
                        "from ( " +
                        "         select l.created, l.plate_number " +
                        "         from event_log l " +
                        "         where l.object_class = 'Gate' " +
                        "           and l.created between :dateFromException and :dateToException " +
                        "           and l.event_type = 'WHITELIST_OUT' " +
                        ") l " +
                        "inner join ( " +
                        "    select cs.id as car_state_id, cs.out_timestamp, cs.car_number " +
                        "    from car_state cs " +
                        "    where cs.out_timestamp between :dateFrom and :dateTo " +
                        "      and cs.out_gate is not null " +
                        ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)";
            } else if("thirdPartyRecords".equals(filterSumReportDto.getEventType())){
                queryString = "select count(distinct cs.car_state_id) " +
                        "from ( " +
                        "         select l.created, l.plate_number " +
                        "         from event_log l " +
                        "         where l.object_class = 'CarState' " +
                        "           and l.created between :dateFromException and :dateToException " +
                        "           and l.event_type = 'PREPAID' " +
                        ") l " +
                        "inner join ( " +
                        "    select cs.id as car_state_id, cs.out_timestamp, cs.car_number " +
                        "    from car_state cs " +
                        "    where cs.out_timestamp between :dateFrom and :dateTo " +
                        "      and cs.out_gate is not null " +
                        ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)";
            } else if("abonementRecords".equals(filterSumReportDto.getEventType())){
                queryString = "select count(distinct cs.car_state_id) " +
                        "from ( " +
                        "         select l.created, l.plate_number " +
                        "         from event_log l " +
                        "         where l.object_class = 'Gate' " +
                        "           and l.created between :dateFromException and :dateToException " +
                        "           and l.event_type = 'ABONEMENT_PASS' " +
                        ") l " +
                        "inner join ( " +
                        "    select cs.id as car_state_id, cs.out_timestamp, cs.car_number " +
                        "    from car_state cs " +
                        "    where cs.out_timestamp between :dateFrom and :dateTo " +
                        "      and cs.out_gate is not null " +
                        ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)";
            } else if("freeMinuteRecords".equals(filterSumReportDto.getEventType())){
                queryString = "select count(distinct cs.car_state_id) " +
                        "from ( " +
                        "         select l.created, l.plate_number " +
                        "         from event_log l " +
                        "         where l.object_class = 'Gate' " +
                        "           and l.created between :dateFromException and :dateToException " +
                        "           and l.event_type = 'FIFTEEN_FREE' " +
                        ") l " +
                        "inner join ( " +
                        "    select cs.id as car_state_id, cs.out_timestamp, cs.car_number " +
                        "    from car_state cs " +
                        "    where cs.out_timestamp between :dateFrom and :dateTo " +
                        "      and cs.out_gate is not null " +
                        ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)";
            } else if("debtRecords".equals(filterSumReportDto.getEventType())){
                queryString = "select count(distinct cs.car_state_id) " +
                        "from ( " +
                        "         select l.created, l.plate_number " +
                        "         from event_log l " +
                        "         where l.object_class = 'Gate' " +
                        "           and l.created between :dateFromException and :dateToException " +
                        "           and l.event_type = 'DEBT_OUT' " +
                        ") l " +
                        "inner join ( " +
                        "    select cs.id as car_state_id, cs.out_timestamp, cs.car_number " +
                        "    from car_state cs " +
                        "    where cs.out_timestamp between :dateFrom and :dateTo " +
                        "      and cs.out_gate is not null " +
                        ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)";
            } else if("fromBalanceRecords".equals(filterSumReportDto.getEventType())){
                queryString = "select count(distinct cs.car_state_id) " +
                        "from ( " +
                        "         select l.created, l.plate_number " +
                        "         from event_log l " +
                        "         where l.object_class = 'Gate' " +
                        "           and l.created between :dateFromException and :dateToException " +
                        "           and l.event_type = 'PAID_PASS' " +
                        ") l " +
                        "inner join ( " +
                        "    select cs.id as car_state_id, cs.out_timestamp, cs.car_number " +
                        "    from car_state cs " +
                        "        left outer join ( " +
                        "            select p.car_state_id " +
                        "            from payments p " +
                        "            where p.out_date between :dateFrom and :dateTo or p.out_date is null " +
                        "            group by p.car_state_id " +
                        "            having sum(p.amount)  > 0 " +
                        "        ) payments on payments.car_state_id = cs.id " +
                        "    where cs.out_timestamp between :dateFrom and :dateTo " +
                        "    and cs.out_gate is not null " +
                        "    and payments.car_state_id is null " +
                        ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)";
            } else if("freeRecords".equals(filterSumReportDto.getEventType())){
                queryString = "select count(distinct cs.car_state_id) " +
                        "from ( " +
                        "         select l.created, l.plate_number " +
                        "         from event_log l " +
                        "         where l.object_class = 'Gate' " +
                        "           and l.created between :dateFromException and :dateToException " +
                        "           and l.event_type = 'FREE_PASS' " +
                        ") l " +
                        "inner join ( " +
                        "    select cs.id as car_state_id, cs.out_timestamp, cs.car_number " +
                        "    from car_state cs " +
                        "    where cs.out_timestamp between :dateFrom and :dateTo " +
                        "      and cs.out_gate is not null " +
                        ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)";
            } else if("autoClosedRecords".equals(filterSumReportDto.getEventType())){
                queryString = "select count(cs.id) " +
                        "from car_state cs " +
                        "where cs.out_timestamp between :dateFrom and :dateTo " +
                        "and cs.out_gate is null";
            }

            Query query = entityManager.createNativeQuery(queryString)
                    .setParameter("dateFrom", filterSumReportDto.getDateFrom())
                    .setParameter("dateTo", filterSumReportDto.getDateTo());

            if(!"records".equals(filterSumReportDto.getEventType()) && !"autoClosedRecords".equals(filterSumReportDto.getEventType())){
                query.setParameter("dateFromException", dateFromException.getTime())
                        .setParameter("dateToException", dateToException.getTime());
            }

            Object result = query.getSingleResult();

            Map<String, String> fieldsMap = new HashMap<>(1);
            fieldsMap.put("result", String.valueOf(result));
            fieldsMap.put("eventType", filterSumReportDto.getEventType());
            firstDto.setResults(fieldsMap);
            results.add(firstDto);
        }

        return results;
    }

    public List<SumReportDto> listDetailed(FilterSumReportDto filterSumReportDto) throws ParseException {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(filterSumReportDto.getDateFrom());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        filterSumReportDto.setDateFrom(calendar.getTime());
        calendar.add(Calendar.DATE, 1);
        filterSumReportDto.setDateTo(calendar.getTime());

        Calendar dateFromCalendar = Calendar.getInstance();
        dateFromCalendar.setTime(filterSumReportDto.getDateFrom());
        dateFromCalendar.add(Calendar.MINUTE, -1);
        Date dateFromException = dateFromCalendar.getTime();

        Calendar dateToCalendar = Calendar.getInstance();
        dateToCalendar.setTime(filterSumReportDto.getDateTo());
        dateToCalendar.add(Calendar.MINUTE, 1);
        Date dateToException = dateToCalendar.getTime();

        String commonPart = " select distinct cs.car_number, DATE_FORMAT(date_add(cs.in_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as inDate, DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as outDate, inGate.name as gateIn, outGate.name as gateOut " +
                "from ( " +
                "         select l.created, l.plate_number " +
                "         from event_log l " +
                "         where l.object_class = 'Gate' " +
                "           and l.created between :dateFromException and :dateToException  " +
                "           INDIVIDUAL_CONDITION " +
                "     ) l " +
                "inner join ( " +
                "    select cs.id as car_state_id, cs.in_timestamp, cs.out_timestamp, cs.car_number, cs.in_gate, cs.out_gate " +
                "    from car_state cs " +
                "    where cs.out_timestamp between :dateFrom and :dateTo " +
                "      and cs.out_gate is not null " +
                ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second) " +
                "left outer join " +
                "     gate inGate on inGate.id = cs.in_gate " +
                "left outer join " +
                "     gate outGate on outGate.id = cs.out_gate; ";
        String queryText = null;

        switch (filterSumReportDto.getEventType()) {
            case  ("whitelistRecords"):
                queryText = commonPart.replaceFirst("INDIVIDUAL_CONDITION","  and l.event_type = 'WHITELIST_OUT'");
                break;
            case  ("thirdPartyRecords"):
                queryText = commonPart.replaceFirst("INDIVIDUAL_CONDITION","  and l.event_type = 'PREPAID'").replaceFirst("where l.object_class = 'Gate'","where l.object_class = 'CarState'");
                break;
            case ("abonementRecords"):
                queryText = commonPart.replaceFirst("INDIVIDUAL_CONDITION","  and l.event_type = 'ABONEMENT_PASS'");
                break;
            case ("freeMinuteRecords"):
                queryText = commonPart.replaceFirst("INDIVIDUAL_CONDITION","  and l.event_type = 'FIFTEEN_FREE'");
                break;
            case ("debtRecords"):
                queryText = commonPart.replaceFirst("INDIVIDUAL_CONDITION","  and l.event_type = 'DEBT_OUT'");
                break;
            case ("freeRecords"):
                queryText = commonPart.replaceFirst("INDIVIDUAL_CONDITION","  and l.event_type = 'FREE_PASS'");
                break;
            case ("paymentRecords"):
                queryText = " select distinct cs.car_number, DATE_FORMAT(date_add(cs.in_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as inDate, DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as outDate, inGate.name as gateIn, outGate.name as gateOut " +
                        "from ( " +
                        "         select l.created, l.plate_number " +
                        "         from event_log l " +
                        "         where l.object_class = 'Gate' " +
                        "           and l.created between :dateFromException and :dateToException " +
                        "           and l.event_type = 'PAID_PASS' " +
                        "     ) l " +
                        "inner join ( " +
                        "     select cs.id as car_state_id, cs.in_timestamp, cs.out_timestamp, cs.car_number, cs.in_gate, cs.out_gate " +
                        "     from car_state cs " +
                        "         inner join ( " +
                        "             select p.car_state_id " +
                        "             from payments p " +
                        "             where p.out_date between :dateFrom and :dateTo or p.out_date is null " +
                        "             group by p.car_state_id " +
                        "             having sum(p.amount)  > 0 " +
                        "         ) payments on payments.car_state_id = cs.id " +
                        "     where cs.out_timestamp between :dateFrom and :dateTo " +
                        "     and cs.out_gate is not null " +
                        " ) cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second) " +
                        "left outer join " +
                        "     gate inGate on inGate.id = cs.in_gate " +
                        "left outer join " +
                        "     gate outGate on outGate.id = cs.out_gate;";
                break;
            case ("fromBalanceRecords"):
                queryText = " select distinct cs.car_number, DATE_FORMAT(date_add(cs.in_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as inDate, DATE_FORMAT(date_add(cs.out_timestamp, INTERVAL 6 hour), '%d.%m.%Y %H:%i') as outDate, inGate.name as gateIn, outGate.name as gateOut " +
                        "from ( " +
                        "         select l.created, l.plate_number " +
                        "         from event_log l " +
                        "         where l.object_class = 'Gate' " +
                        "           and l.created between :dateFromException and :dateToException " +
                        "           and l.event_type = 'PAID_PASS' " +
                        "     ) l " +
                        "inner join ( " +
                        "    select cs.id as car_state_id, cs.out_timestamp, cs.car_number, cs.in_timestamp, cs.in_gate, cs.out_gate " +
                        "    from car_state cs " +
                        "        left outer join ( " +
                        "            select p.car_state_id " +
                        "            from payments p " +
                        "            where p.out_date between :dateFrom and :dateTo or p.out_date is null " +
                        "            group by p.car_state_id " +
                        "            having sum(p.amount)  > 0 " +
                        "        ) payments on payments.car_state_id = cs.id " +
                        "    where cs.out_timestamp between :dateFrom and :dateTo " +
                        "    and cs.out_gate is not null " +
                        "    and payments.car_state_id is null " +
                        " ) cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second) " +
                        "left outer join " +
                        "     gate inGate on inGate.id = cs.in_gate " +
                        "left outer join " +
                        "     gate outGate on outGate.id = cs.out_gate;";

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

        Query query = entityManager
                .createNativeQuery(queryText)
                .setParameter("dateFrom", filterSumReportDto.getDateFrom())
                .setParameter("dateTo", filterSumReportDto.getDateTo());

        if(!"autoClosedRecords".equals(filterSumReportDto.getEventType())){
            query.setParameter("dateFromException", dateFromException).setParameter("dateToException", dateToException);
        }

        List<Object[]> queryResult = query.getResultList();

        List<SumReportDto> listResult = new ArrayList<>(queryResult.size());
        SumReportDto sumReportDto = new SumReportDto();

        List<SumReportListDto> sumReportList  = new ArrayList<>(queryResult.size());

        for(Object[] object: queryResult){
            int it = 0;

            SumReportListDto dto = new SumReportListDto();
            dto.setPlateNumber((String)object[it++]);
            dto.setFormattedInDate((String)object[it++]);
            dto.setFormattedOutDate((String)object[it++]);
            dto.setInPlace((String)object[it++]);
            dto.setOutPlace((String)object[it++]);
            sumReportList.add(dto);
        }
        sumReportDto.setListResult(sumReportList);
        listResult.add(sumReportDto);

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

    private RootServicesGetterService getRootServicesGetterService() {
        if (rootServicesGetterService == null) {
            rootServicesGetterService = (RootServicesGetterService) ReportPlugin.INSTANCE.getApplicationContext().getBean("rootServicesGetterServiceImpl");
        }
        return rootServicesGetterService;
    }

    private PluginService getPluginService(){
        if(pluginService==null){
            pluginService = getRootServicesGetterService().getPluginService();
        }

        if (pluginService == null) {
            pluginService = (PluginService) ReportPlugin.INSTANCE.getMainApplicationContext().getBean("pluginServiceImpl");
        }

        return pluginService;
    }
}
