package kz.spt.app.service.impl;

import kz.spt.app.repository.CarStateRepository;
import kz.spt.app.repository.ParkingRepository;
import kz.spt.lib.extension.PluginRegister;
import kz.spt.lib.model.dto.dashboard.DashboardOccupancyDto;
import kz.spt.lib.service.DashboardService;
import kz.spt.lib.service.PluginService;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;
import java.util.*;

@Log
@Service
public class DashboardServiceImpl implements DashboardService {

    private CarStateRepository carStateRepository;
    private ParkingRepository parkingRepository;

    private PluginService pluginService;

    @PersistenceContext
    private EntityManager entityManager;

    private int timezoneShift = 0;


    public DashboardServiceImpl(CarStateRepository carStateRepository, ParkingRepository parkingRepository, PluginService pluginService){
        this.carStateRepository = carStateRepository;
        this.parkingRepository = parkingRepository;
        this.pluginService = pluginService;

        LocalDateTime current = LocalDateTime.now();
        ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(current);
        int seconds = zoneOffset.getTotalSeconds();
        int minutes = seconds / 60;
        int timezoneShift = minutes / 60;
    }

    @Override
    public DashboardOccupancyDto freePercentageByTotal() {
        Long totalOccupancy = parkingRepository.totalOccupancy();
        Long occupied = carStateRepository.countTotalParked();

        if(totalOccupancy == null || 0L == totalOccupancy){
            return null;
        }
        if(occupied > totalOccupancy){
            totalOccupancy = occupied;
        }
        DashboardOccupancyDto dto = new DashboardOccupancyDto(totalOccupancy, occupied, (occupied*100)/totalOccupancy);

        return dto;
    }

    @Override
    public List incomeByProviders(String period, String from, String to)  {

        LocalDateTime current = LocalDateTime.now();

        if("year".equals(period)){
            current = LocalDateTime.of(current.getYear(), 1, 1, 0, 0);
        } else if("month".equals(period)){
            current = LocalDateTime.of(current.getYear(), current.getMonth(), 1, 0, 0);
        } else if("week".equals(period)){
            current = current.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
        } else if("day".equals(period)){
            current = current.truncatedTo(ChronoUnit.DAYS);
        } else if("day".equals(period)){
            current = current.truncatedTo(ChronoUnit.DAYS);
        }

        Date fromDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());
        Date toDate = null;

        if("period".equals(period)){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            fromDate = Date.from(LocalDate.parse(from, formatter).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            toDate = Date.from(LocalDate.parse(to, formatter).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        }

        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
        if (billingPluginRegister != null) {
            String queryString = "select PERIOD, pp.name, coalesce(sum(p.amount), 0) " +
                    "from payments p " +
                    "    inner join payment_provider pp on p.provider_id = pp.id " +
                    "where p.created >= :fromDate " +
                    "group by PERIOD, pp.name " +
                    "order by PERIOD, pp.name ";
            if("year".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%m')");
            } else if("month".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%d')");
            } else if("week".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%a')");
            } else if("day".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%H')");
            } else if("period".equals(period)){
                queryString =  queryString.replace("p.created >= :fromDate", "p.created >= :fromDate and p.created  <= :toDate");
                Long diff = toDate.getTime() - fromDate.getTime();
                long days = diff / (1000*60*60*24);
                if(days > 1d){
                    if(days > 7d){
                        if(days > 31d){
                            queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%m')");
                        } else {
                            queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%d')");
                        }
                    } else {
                        queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%a')");
                    }
                } else {
                    queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%H')");
                }
            }

            log.info("queryString: " + queryString);

            Query query = entityManager.createNativeQuery(queryString).setParameter("fromDate", fromDate);
            if("period".equals(period)){
                query.setParameter("toDate", toDate);
            }
            return query.getResultList();
        }

        return null;
    }

    @Override
    public List countPaymentsByProviders(String period, String from, String to) {
        LocalDateTime current = LocalDateTime.now();

        if("year".equals(period)){
            current = LocalDateTime.of(current.getYear(), 1, 1, 0, 0);
        } else if("month".equals(period)){
            current = LocalDateTime.of(current.getYear(), current.getMonth(), 1, 0, 0);
        } else if("week".equals(period)){
            current = current.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
        } else if("day".equals(period)){
            current = current.truncatedTo(ChronoUnit.DAYS);
        }

        Date fromDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());
        Date toDate = null;

        if("period".equals(period)){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            fromDate = Date.from(LocalDate.parse(from, formatter).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            toDate = Date.from(LocalDate.parse(to, formatter).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        }

        PluginRegister billingPluginRegister = pluginService.getPluginRegister(StaticValues.billingPlugin);
        if (billingPluginRegister != null) {
            String queryString = "select PERIOD, pp.name, coalesce(count(p.id), 0) " +
                    "from payments p " +
                    "    inner join payment_provider pp on p.provider_id = pp.id " +
                    "where p.created >= :fromDate " +
                    "group by PERIOD, pp.name " +
                    "order by PERIOD, pp.name ";
            if("year".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%m')");
            } else if("month".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%d')");
            } else if("week".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%a')");
            } else if("day".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%H')");
            } else if("period".equals(period)){
                queryString =  queryString.replace("p.created >= :fromDate", "p.created >= :fromDate and p.created  <= :toDate");
                Long diff = toDate.getTime() - fromDate.getTime();
                long days = diff / (1000*60*60*24);
                if(days > 1d){
                    if(days > 7d){
                        if(days > 31d){
                            queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%m')");
                        } else {
                            queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%d')");
                        }
                    } else {
                        queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%a')");
                    }
                } else {
                    queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift < 0 ? "date_add" : "date_sub") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%H')");
                }
            }

            log.info("queryString: " + queryString);

            Query query = entityManager.createNativeQuery(queryString).setParameter("fromDate", fromDate);
            if("period".equals(period)){
                query.setParameter("toDate", toDate);
            }
            return query.getResultList();
        }

        return null;
    }

    @Override
    public List occupancyInPeriod(String period, String from, String to) {
        LocalDateTime current = LocalDateTime.now();
        LocalDateTime till = LocalDateTime.now();
        List<String> fields = new ArrayList<>();

        if("year".equals(period)){
            current = LocalDateTime.of(current.getYear(), 1, 1, 0, 0);
        } else if("month".equals(period)){
            current = LocalDateTime.of(current.getYear(), current.getMonth(), 1, 0, 0);
        } else if("week".equals(period)){
            current = current.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
        } else if("day".equals(period)){
            current = current.truncatedTo(ChronoUnit.DAYS);
        } else if("period".equals(period)){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            current = LocalDate.parse(from, formatter).atStartOfDay();
            till = LocalDate.parse(to, formatter).atStartOfDay();
        }

        Date fromDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(till.atZone(ZoneId.systemDefault()).toInstant());

        String queryString = "select " +
                "PERIOD" +
                " from car_state cs" +
                " where (:fromDate between cs.in_timestamp and cs.out_timestamp)" +
                " or (:toDate between cs.in_timestamp and cs.out_timestamp)" +
                " or (:toDate >= cs.in_timestamp and cs.out_timestamp is null)" +
                " or (:fromDate <= cs.in_timestamp and :toDate >= cs.out_timestamp)";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if(timezoneShift >= 0){
            current = current.minusHours(timezoneShift);
            till = till.minusHours(timezoneShift);
        } else if(timezoneShift < 0){
            current = current.plusHours(Math.abs(timezoneShift));
            till = till.plusHours(Math.abs(timezoneShift));
        }

        StringBuilder sum = new StringBuilder("");
        if("year".equals(period)){
            while(current.isBefore(till)){
                if(sum.length() > 0) sum.append(" ,");

                sum.append(" sum(case when (cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusMonths(1).format(formatter) + "') then 1 else 0 end) as '" + current.getMonth() + "'");
                fields.add(current.getMonth().toString());
                current = current.plusMonths(1);
            }
            queryString = queryString.replace("PERIOD", sum.toString());
        } else if("month".equals(period)){
            while(current.isBefore(till)){
                if(sum.length() > 0) sum.append(" ,");

                sum.append(" sum(case when (cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "') then 1 else 0 end) as '" + current.getDayOfMonth() + "'");
                fields.add(String.valueOf(current.getDayOfMonth()));
                current = current.plusDays(1);
            }
            queryString = queryString.replace("PERIOD", sum.toString());
        } else if("week".equals(period)){
            while(current.isBefore(till)){
                if(sum.length() > 0) sum.append(" ,");

                sum.append(" sum(case when (cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "') then 1 else 0 end) as '" + current.getDayOfWeek() + "'");
                fields.add(current.getDayOfWeek().toString());
                current = current.plusDays(1);
            }
            queryString = queryString.replace("PERIOD", sum.toString());
        } else if("day".equals(period)){
            while(current.isBefore(till)){
                if(sum.length() > 0) sum.append(" ,");

                sum.append(" sum(case when (cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "') then 1 else 0 end) as '" + current.getHour() + "'");
                fields.add(String.valueOf(current.getHour()));
                current = current.plusHours(1);
            }
            queryString = queryString.replace("PERIOD", sum.toString());
        } else if("period".equals(period)){
            Long diff = toDate.getTime() - fromDate.getTime();
            long days = diff / (1000*60*60*24);
            if(days > 1d){
                if(days > 7d){
                    if(days > 31d){
                        while(current.isBefore(till)){
                            if(sum.length() > 0) sum.append(" ,");

                            sum.append(" sum(case when (cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusMonths(1).format(formatter) + "') then 1 else 0 end) as '" + current.getMonth() + "'");
                            fields.add(current.getMonth().toString());
                            current = current.plusMonths(1);
                        }
                        queryString = queryString.replace("PERIOD", sum.toString());
                    } else {
                        while(current.isBefore(till)){
                            if(sum.length() > 0) sum.append(" ,");

                            sum.append(" sum(case when (cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "') then 1 else 0 end) as '" + current.getDayOfMonth() + "'");
                            fields.add(String.valueOf(current.getDayOfMonth()));
                            current = current.plusDays(1);
                        }
                        queryString = queryString.replace("PERIOD", sum.toString());
                    }
                } else {
                    while(current.isBefore(till)){
                        if(sum.length() > 0) sum.append(" ,");

                        sum.append(" sum(case when (cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "') then 1 else 0 end) as '" + current.getDayOfWeek() + "'");
                        fields.add(current.getDayOfWeek().toString());
                        current = current.plusDays(1);
                    }
                    queryString = queryString.replace("PERIOD", sum.toString());
                }
            } else {
                while(current.isBefore(till)){
                    if(sum.length() > 0) sum.append(" ,");

                    sum.append(" sum(case when (cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "') then 1 else 0 end) as '" + current.getHour() + "'");
                    fields.add(String.valueOf(current.getHour()));
                    current = current.plusHours(1);
                }
                queryString = queryString.replace("PERIOD", sum.toString());
            }
        }

        log.info("queryString: " + queryString);

        List<Object> result = new ArrayList<>();
        List queryResult =  entityManager.createNativeQuery(queryString).setParameter("fromDate", fromDate).setParameter("toDate", toDate).getResultList();

        result.add(fields);
        result.add(queryResult);
        return result;
    }

    @Override
    public List passByGatesInPeriod(String period, String from, String to) {
        LocalDateTime current = LocalDateTime.now();
        LocalDateTime till = LocalDateTime.now();

        if("year".equals(period)){
            current = LocalDateTime.of(current.getYear(), 1, 1, 0, 0);
        } else if("month".equals(period)){
            current = LocalDateTime.of(current.getYear(), current.getMonth(), 1, 0, 0);
        } else if("week".equals(period)){
            current = current.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
        } else if("day".equals(period)){
            current = current.truncatedTo(ChronoUnit.DAYS);
        } else if("period".equals(period)){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            current = LocalDate.parse(from, formatter).atStartOfDay();
            till = LocalDate.parse(to, formatter).atStartOfDay();
        }

        Date fromDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(till.atZone(ZoneId.systemDefault()).toInstant());

        String entryQueryString = "select g.id, g.name, count(g.id)" +
                " from car_state cs " +
                "    inner join gate g on cs.in_gate = g.id" +
                " where cs.in_timestamp between :fromDate and :toDate" +
                " and cs.in_gate is not null" +
                " group by g.id, g.name";

        String exitQueryString = "select g.id, g.name, count(g.id)" +
                " from car_state cs" +
                "         inner join gate g on cs.out_gate = g.id" +
                " where cs.in_timestamp between :fromDate and :toDate" +
                " and cs.out_gate is not null" +
                " group by g.id, g.name";

        List entryResult =  entityManager.createNativeQuery(entryQueryString).setParameter("fromDate", fromDate).setParameter("toDate", toDate).getResultList();
        List exitResult =  entityManager.createNativeQuery(exitQueryString).setParameter("fromDate", fromDate).setParameter("toDate", toDate).getResultList();

        entryResult.addAll(exitResult);
        return entryResult;
    }

    @Override
    public List durationsInPeriod(String period, String from, String to) {
        LocalDateTime current = LocalDateTime.now();
        LocalDateTime till = LocalDateTime.now();
        List<String> fields = new ArrayList<>();

        if("year".equals(period)){
            current = LocalDateTime.of(current.getYear(), 1, 1, 0, 0);
        } else if("month".equals(period)){
            current = LocalDateTime.of(current.getYear(), current.getMonth(), 1, 0, 0);
        } else if("week".equals(period)){
            current = current.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
        } else if("day".equals(period)){
            current = current.truncatedTo(ChronoUnit.DAYS);
        } else if("period".equals(period)){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            current = LocalDate.parse(from, formatter).atStartOfDay();
            till = LocalDate.parse(to, formatter).atStartOfDay();
        }

        Date fromDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(till.atZone(ZoneId.systemDefault()).toInstant());

        String queryString = "select " +
                "PERIOD" +
                " from car_state cs" +
                " where (:fromDate between cs.in_timestamp and cs.out_timestamp)" +
                " or (:toDate between cs.in_timestamp and cs.out_timestamp)" +
                " or (:toDate >= cs.in_timestamp and cs.out_timestamp is null)" +
                " or (:fromDate <= cs.in_timestamp and :toDate >= cs.out_timestamp)";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if(timezoneShift >= 0){
            current = current.minusHours(timezoneShift);
            till = till.minusHours(timezoneShift);
        } else if(timezoneShift < 0){
            current = current.plusHours(Math.abs(timezoneShift));
            till = till.plusHours(Math.abs(timezoneShift));
        }

        StringBuilder sum = new StringBuilder("");
        if("year".equals(period)){
            while(current.isBefore(till)){
                if(sum.length() > 0) sum.append(" ,");

                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 1 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusMonths(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 1 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 2 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusMonths(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 2 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 3 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusMonths(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 3 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusMonths(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusMonths(1).format(formatter) + "')) then 1 else 0 end)");
                fields.add(current.getMonth().toString());
                current = current.plusMonths(1);
            }
            queryString = queryString.replace("PERIOD", sum.toString());
        } else if("month".equals(period)){
            while(current.isBefore(till)){
                if(sum.length() > 0) sum.append(" ,");

                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 1 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 1 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 2 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 2 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 3 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 3 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end)");

                fields.add(String.valueOf(current.getDayOfMonth()));
                current = current.plusDays(1);
            }
            queryString = queryString.replace("PERIOD", sum.toString());
        } else if("week".equals(period)){
            while(current.isBefore(till)){
                if(sum.length() > 0) sum.append(" ,");

                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 1 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 1 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 2 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 2 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 3 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 3 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end)");
                
                fields.add(current.getDayOfWeek().toString());
                current = current.plusDays(1);
            }
            queryString = queryString.replace("PERIOD", sum.toString());
        } else if("day".equals(period)){
            while(current.isBefore(till)){
                if(sum.length() > 0) sum.append(" ,");

                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 1 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 1 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 2 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 2 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 3 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 3 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "')) then 1 else 0 end), ");
                sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "')) then 1 else 0 end)");
                
                fields.add(String.valueOf(current.getHour()));
                current = current.plusHours(1);
            }
            queryString = queryString.replace("PERIOD", sum.toString());
        } else if("period".equals(period)){
            Long diff = toDate.getTime() - fromDate.getTime();
            long days = diff / (1000*60*60*24);
            if(days > 1d){
                if(days > 7d){
                    if(days > 31d){
                        while(current.isBefore(till)){
                            if(sum.length() > 0) sum.append(" ,");

                            sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 1 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusMonths(1).format(formatter) + "')) then 1 else 0 end), ");
                            sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 1 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 2 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusMonths(1).format(formatter) + "')) then 1 else 0 end), ");
                            sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 2 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 3 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusMonths(1).format(formatter) + "')) then 1 else 0 end), ");
                            sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 3 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusMonths(1).format(formatter) + "')) then 1 else 0 end), ");
                            sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusMonths(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusMonths(1).format(formatter) + "')) then 1 else 0 end)");
                            fields.add(current.getMonth().toString());
                            current = current.plusMonths(1);
                        }
                        queryString = queryString.replace("PERIOD", sum.toString());
                    } else {
                        while(current.isBefore(till)){
                            if(sum.length() > 0) sum.append(" ,");

                            sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 1 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                            sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 1 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 2 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                            sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 2 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 3 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                            sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 3 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                            sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end)");
                            fields.add(String.valueOf(current.getDayOfMonth()));
                            current = current.plusDays(1);
                        }
                        queryString = queryString.replace("PERIOD", sum.toString());
                    }
                } else {
                    while(current.isBefore(till)){
                        if(sum.length() > 0) sum.append(" ,");

                        sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 1 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                        sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 1 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 2 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                        sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 2 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 3 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                        sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 3 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end), ");
                        sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusDays(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusDays(1).format(formatter) + "')) then 1 else 0 end)");
                        fields.add(current.getDayOfWeek().toString());
                        current = current.plusDays(1);
                    }
                    queryString = queryString.replace("PERIOD", sum.toString());
                }
            } else {
                while(current.isBefore(till)){
                    if(sum.length() > 0) sum.append(" ,");

                    sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 1 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "')) then 1 else 0 end), ");
                    sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 1 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 2 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "')) then 1 else 0 end), ");
                    sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 2 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 3 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "')) then 1 else 0 end), ");
                    sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 3 and TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) < 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "')) then 1 else 0 end), ");
                    sum.append(" sum(case when TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) >= 4 and ((cs.in_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "')) then 1 else 0 end)");

                    fields.add(String.valueOf(current.getHour()));
                    current = current.plusHours(1);
                }
                queryString = queryString.replace("PERIOD", sum.toString());
            }
        }

        log.info("queryString: " + queryString);

        List<Object> result = new ArrayList<>();
        List queryResult =  entityManager.createNativeQuery(queryString).setParameter("fromDate", fromDate).setParameter("toDate", toDate).getResultList();

        result.add(fields);
        result.add(queryResult);
        return result;
    }

    @Override
    public Map occupancyAndMoneyByPeriod(String period, String from, String to) {

        LocalDateTime current = LocalDateTime.now();
        LocalDateTime till = LocalDateTime.now();

        if("year".equals(period)){
            current = LocalDateTime.of(current.getYear(), 1, 1, 0, 0);
        } else if("month".equals(period)){
            current = LocalDateTime.of(current.getYear(), current.getMonth(), 1, 0, 0);
        } else if("week".equals(period)){
            current = current.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
        } else if("day".equals(period)){
            current = current.truncatedTo(ChronoUnit.DAYS);
        } else if("period".equals(period)){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            current = LocalDate.parse(from, formatter).atStartOfDay();
            till = LocalDate.parse(to, formatter).atStartOfDay();
        }

        Date fromDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(till.atZone(ZoneId.systemDefault()).toInstant());

        Calendar dateFromException = Calendar.getInstance();
        dateFromException.setTime(fromDate);
        dateFromException.add(Calendar.MINUTE, -1);

        Calendar dateToException = Calendar.getInstance();
        dateToException.setTime(toDate);
        dateToException.add(Calendar.MINUTE, 1);

        Map<String, Object> fieldsMap = new HashMap<>(15);

        String recordsQueryString = "select count(cs.id) as count from car_state cs where cs.out_timestamp between :fromDate and :toDate";
        List<Long> recordsList = entityManager.createNativeQuery(recordsQueryString)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();

        Map<String, Object> fields = new HashMap<>();
        fieldsMap.put("records", recordsList);

        String paymentRecordsQueryString = "select count(distinct cs.car_state_id), sum(cs.totalSumma)" +
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
        List<Object> paymentRecordsList = entityManager.createNativeQuery(paymentRecordsQueryString)
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("paymentRecords", paymentRecordsList);

        String whitelistRecordsQueryString = "select count(distinct cs.car_state_id) " +
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

        List<Object> whitelistRecordsList = entityManager.createNativeQuery(whitelistRecordsQueryString)
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("whitelistRecords", whitelistRecordsList);

        PluginRegister megaPluginRegister = pluginService.getPluginRegister(StaticValues.megaPlugin);
        if(megaPluginRegister != null){
            String thirdPartyRecordsQueryString = "select count(distinct cs.car_state_id) " +
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

            List<Object> thirdPartyRecordsList = entityManager.createNativeQuery(thirdPartyRecordsQueryString)
                    .setParameter("dateFrom", fromDate)
                    .setParameter("dateTo", toDate)
                    .setParameter("dateFromException", dateFromException.getTime())
                    .setParameter("dateToException", dateToException.getTime())
                    .getResultList();

            fieldsMap.put("thirdPartyRecords", thirdPartyRecordsList);
        }

        String abonementRecordsQueryString = "select count(distinct cs.car_state_id) " +
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

        List<Object> abonementRecordsList = entityManager.createNativeQuery(abonementRecordsQueryString)
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("abonementRecords", abonementRecordsList);

        String freeMinuteRecordsQueryString = "select count(distinct cs.car_state_id) " +
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

        List<Object> freeMinuteRecordsList = entityManager.createNativeQuery(freeMinuteRecordsQueryString)
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("freeMinuteRecords", freeMinuteRecordsList);

        String debtRecordsQueryString = "select count(distinct cs.car_state_id) " +
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

        List<Object> debtRecordsList = entityManager.createNativeQuery(debtRecordsQueryString)
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("debtRecords", debtRecordsList);

        String fromBalanceRecordsQueryString = "select count(distinct cs.car_state_id) " +
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

        List<Object> fromBalanceRecordsList = entityManager.createNativeQuery(fromBalanceRecordsQueryString)
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("fromBalanceRecords", fromBalanceRecordsList);

        String freeRecordsQueryString = "select count(distinct cs.car_state_id) " +
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

        List<Object> freeRecordsList = entityManager.createNativeQuery(freeRecordsQueryString)
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("freeRecords", freeRecordsList);

        String autoClosedRecordsQueryString = "select count(cs.id) " +
                "from car_state cs " +
                "where cs.out_timestamp between :dateFrom and :dateTo " +
                "and cs.out_gate is null";

        List<Object> autoClosedRecordsList = entityManager.createNativeQuery(autoClosedRecordsQueryString)
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .getResultList();

        fieldsMap.put("autoClosedRecords", autoClosedRecordsList);

        return fieldsMap;
    }

    @Override
    public Map realTimeOccupancy() {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");

        LocalDateTime till = LocalDateTime.now();
        LocalDateTime from = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.HOURS);

        Date fromDate = Date.from(from.atZone(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(till.atZone(ZoneId.systemDefault()).toInstant());

        Calendar dateFromException = Calendar.getInstance();
        dateFromException.setTime(fromDate);
        dateFromException.add(Calendar.MINUTE, -1);

        Calendar dateToException = Calendar.getInstance();
        dateToException.setTime(toDate);
        dateToException.add(Calendar.MINUTE, 1);

        if(timezoneShift >= 0){
            from = from.minusHours(timezoneShift);
            till = till.minusHours(timezoneShift);
        } else if(timezoneShift < 0){
            from = from.plusHours(timezoneShift*(-1));
            till = till.plusHours(timezoneShift*(-1));
        }

        Map<String, Object> fieldsMap = new HashMap<>(15);

        String recordsQueryString = "select count(cs.id) as count from car_state cs where cs.in_timestamp between :fromDate and :toDate";
        List<Long> recordsList = entityManager.createNativeQuery(recordsQueryString)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();

        List<String> fields = new ArrayList<>();
        fieldsMap.put("totalRecords", recordsList);

        StringBuilder paymentRecordsQueryString = new StringBuilder("");
        StringBuilder whitelistRecordsQueryString = new StringBuilder("");
        StringBuilder thirdPartyRecordsQueryString = new StringBuilder("");
        StringBuilder abonementRecordsQueryString = new StringBuilder("");
        StringBuilder freeMinuteRecordsQueryString = new StringBuilder("");
        StringBuilder debtRecordsQueryString = new StringBuilder("");
        StringBuilder fromBalanceRecordsQueryString = new StringBuilder("");
        StringBuilder freeRecordsQueryString = new StringBuilder("");
        StringBuilder autoClosedRecordsQueryString = new StringBuilder("");

        while(from.isBefore(till)){

            if(timezoneShift >= 0){
                fields.add(from.plusHours(timezoneShift).format(labelFormatter));
            } else if(timezoneShift < 0){
                fields.add(from.minusHours(timezoneShift*(-1)).format(labelFormatter));
            }

            if(paymentRecordsQueryString.length() > 0) {
                paymentRecordsQueryString.append(" ,");
            } else {
                paymentRecordsQueryString.append("select ");
            }
            paymentRecordsQueryString.append("sum(case when cs.in_timestamp < '" + from.plusHours(1).format(formatter) + "' and cs.out_timestamp >= '" + from.format(formatter) + "' then 1 else 0 end) ");

            if(whitelistRecordsQueryString.length() > 0) {
                whitelistRecordsQueryString.append(" ,");
            } else {
                whitelistRecordsQueryString.append("select ");
            }
            whitelistRecordsQueryString.append("sum(case when cs.in_timestamp < '" + from.plusHours(1).format(formatter) + "' and cs.out_timestamp >= '" + from.format(formatter) + "' then 1 else 0 end) ");

            if(thirdPartyRecordsQueryString.length() > 0) {
                thirdPartyRecordsQueryString.append(" ,");
            } else {
                thirdPartyRecordsQueryString.append("select ");
            }
            thirdPartyRecordsQueryString.append("sum(case when cs.in_timestamp < '" + from.plusHours(1).format(formatter) + "' and cs.out_timestamp >= '" + from.format(formatter) + "' then 1 else 0 end) ");

            if(abonementRecordsQueryString.length() > 0) {
                abonementRecordsQueryString.append(" ,");
            } else {
                abonementRecordsQueryString.append("select ");
            }
            abonementRecordsQueryString.append("sum(case when cs.in_timestamp < '" + from.plusHours(1).format(formatter) + "' and cs.out_timestamp >= '" + from.format(formatter) + "' then 1 else 0 end) ");

            if(freeMinuteRecordsQueryString.length() > 0) {
                freeMinuteRecordsQueryString.append(" ,");
            } else {
                freeMinuteRecordsQueryString.append("select ");
            }
            freeMinuteRecordsQueryString.append("sum(case when cs.in_timestamp < '" + from.plusHours(1).format(formatter) + "' and cs.out_timestamp >= '" + from.format(formatter) + "' then 1 else 0 end) ");

            if(debtRecordsQueryString.length() > 0) {
                debtRecordsQueryString.append(" ,");
            } else {
                debtRecordsQueryString.append("select ");
            }
            debtRecordsQueryString.append("sum(case when cs.in_timestamp < '" + from.plusHours(1).format(formatter) + "' and cs.out_timestamp >= '" + from.format(formatter) + "' then 1 else 0 end) ");

            if(fromBalanceRecordsQueryString.length() > 0) {
                fromBalanceRecordsQueryString.append(" ,");
            } else {
                fromBalanceRecordsQueryString.append("select ");
            }
            fromBalanceRecordsQueryString.append("sum(case when cs.in_timestamp < '" + from.plusHours(1).format(formatter) + "' and cs.out_timestamp >= '" + from.format(formatter) + "' then 1 else 0 end) ");

            if(freeRecordsQueryString.length() > 0) {
                freeRecordsQueryString.append(" ,");
            } else {
                freeRecordsQueryString.append("select ");
            }
            freeRecordsQueryString.append("sum(case when cs.in_timestamp < '" + from.plusHours(1).format(formatter) + "' and cs.out_timestamp >= '" + from.format(formatter) + "' then 1 else 0 end) ");

            if(autoClosedRecordsQueryString.length() > 0) {
                autoClosedRecordsQueryString.append(" ,");
            } else {
                autoClosedRecordsQueryString.append("select ");
            }
            autoClosedRecordsQueryString.append("sum(case when cs.in_timestamp < '" + from.plusHours(1).format(formatter) + "' then 1 else 0 end) ");

            from = from.plusHours(1);
        }

        fieldsMap.put("labels", fields);

        paymentRecordsQueryString.append(" from ( " +
                "         select l.created, l.plate_number " +
                "         from event_log l " +
                "         where l.object_class = 'Gate' " +
                "           and l.created between :dateFromException and :dateToException " +
                "           and l.event_type = 'PAID_PASS' " +
                ") l " +
                "inner join ( " +
                "    select p.car_state_id as car_state_id, cs.in_timestamp, cs.out_timestamp, cs.car_number, sum(p.amount) as totalSumma " +
                "    from payments p " +
                "             inner join car_state cs on cs.id = p.car_state_id " +
                "    where (p.out_date between :dateFrom and :dateTo or p.out_date is null) " +
                "      and cs.in_timestamp between :dateFrom and :dateTo " +
                "      and cs.in_timestamp is not null " +
                "      and cs.out_gate is not null " +
                "    group by p.car_state_id " +
                "    having totalSumma > 0 " +
                ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second) ");
        List<Object> paymentRecordsList = entityManager.createNativeQuery(paymentRecordsQueryString.toString())
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("paymentRecords", paymentRecordsList);

        whitelistRecordsQueryString.append(" from ( " +
                "         select l.created, l.plate_number " +
                "         from event_log l " +
                "         where l.object_class = 'Gate' " +
                "           and l.created between :dateFromException and :dateToException " +
                "           and l.event_type = 'WHITELIST_OUT' " +
                ") l " +
                "inner join ( " +
                "    select cs.id as car_state_id, cs.in_timestamp, cs.out_timestamp, cs.car_number " +
                "    from car_state cs " +
                "    where cs.out_timestamp between :dateFrom and :dateTo " +
                "      and cs.out_gate is not null " +
                ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)");

        log.info("whitelistRecordsQueryString: " + whitelistRecordsQueryString);

        List<Object> whitelistRecordsList = entityManager.createNativeQuery(whitelistRecordsQueryString.toString())
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("whitelistRecords", whitelistRecordsList);

        PluginRegister megaPluginRegister = pluginService.getPluginRegister(StaticValues.megaPlugin);
        if(megaPluginRegister != null){
            thirdPartyRecordsQueryString.append(" from ( " +
                    "         select l.created, l.plate_number " +
                    "         from event_log l " +
                    "         where l.object_class = 'CarState' " +
                    "           and l.created between :dateFromException and :dateToException " +
                    "           and l.event_type = 'PREPAID' " +
                    ") l " +
                    "inner join ( " +
                    "    select cs.id as car_state_id, cs.in_timestamp, cs.out_timestamp, cs.car_number " +
                    "    from car_state cs " +
                    "    where cs.out_timestamp between :dateFrom and :dateTo " +
                    "      and cs.out_gate is not null " +
                    ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)");

            log.info("thirdPartyRecordsQueryString: " + thirdPartyRecordsQueryString.toString());

            List<Object> thirdPartyRecordsList = entityManager.createNativeQuery(thirdPartyRecordsQueryString.toString())
                    .setParameter("dateFrom", fromDate)
                    .setParameter("dateTo", toDate)
                    .setParameter("dateFromException", dateFromException.getTime())
                    .setParameter("dateToException", dateToException.getTime())
                    .getResultList();

            fieldsMap.put("thirdPartyRecords", thirdPartyRecordsList);
        }

        abonementRecordsQueryString.append("from ( " +
                "         select l.created, l.plate_number " +
                "         from event_log l " +
                "         where l.object_class = 'Gate' " +
                "           and l.created between :dateFromException and :dateToException " +
                "           and l.event_type = 'ABONEMENT_PASS' " +
                ") l " +
                "inner join ( " +
                "    select cs.id as car_state_id, cs.in_timestamp, cs.out_timestamp, cs.car_number " +
                "    from car_state cs " +
                "    where cs.out_timestamp between :dateFrom and :dateTo " +
                "      and cs.out_gate is not null " +
                ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)");

        List<Object> abonementRecordsList = entityManager.createNativeQuery(abonementRecordsQueryString.toString())
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("abonementRecords", abonementRecordsList);

        freeMinuteRecordsQueryString.append("from ( " +
                "         select l.created, l.plate_number " +
                "         from event_log l " +
                "         where l.object_class = 'Gate' " +
                "           and l.created between :dateFromException and :dateToException " +
                "           and l.event_type = 'FIFTEEN_FREE' " +
                ") l " +
                "inner join ( " +
                "    select cs.id as car_state_id, cs.in_timestamp, cs.out_timestamp, cs.car_number " +
                "    from car_state cs " +
                "    where cs.out_timestamp between :dateFrom and :dateTo " +
                "      and cs.out_gate is not null " +
                ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)");

        List<Object> freeMinuteRecordsList = entityManager.createNativeQuery(freeMinuteRecordsQueryString.toString())
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("freeMinuteRecords", freeMinuteRecordsList);

        debtRecordsQueryString.append("from ( " +
                "         select l.created, l.plate_number " +
                "         from event_log l " +
                "         where l.object_class = 'Gate' " +
                "           and l.created between :dateFromException and :dateToException " +
                "           and l.event_type = 'DEBT_OUT' " +
                ") l " +
                "inner join ( " +
                "    select cs.id as car_state_id, cs.in_timestamp, cs.out_timestamp, cs.car_number " +
                "    from car_state cs " +
                "    where cs.out_timestamp between :dateFrom and :dateTo " +
                "      and cs.out_gate is not null " +
                ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)");

        List<Object> debtRecordsList = entityManager.createNativeQuery(debtRecordsQueryString.toString())
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("debtRecords", debtRecordsList);

        fromBalanceRecordsQueryString.append("from ( " +
                "         select l.created, l.plate_number " +
                "         from event_log l " +
                "         where l.object_class = 'Gate' " +
                "           and l.created between :dateFromException and :dateToException " +
                "           and l.event_type = 'PAID_PASS' " +
                ") l " +
                "inner join ( " +
                "    select cs.id as car_state_id, cs.in_timestamp, cs.out_timestamp, cs.car_number " +
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
                ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)");
        
        List<Object> fromBalanceRecordsList = entityManager.createNativeQuery(fromBalanceRecordsQueryString.toString())
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("fromBalanceRecords", fromBalanceRecordsList);

        freeRecordsQueryString.append("from ( " +
                "         select l.created, l.plate_number " +
                "         from event_log l " +
                "         where l.object_class = 'Gate' " +
                "           and l.created between :dateFromException and :dateToException " +
                "           and l.event_type = 'FREE_PASS' " +
                ") l " +
                "inner join ( " +
                "    select cs.id as car_state_id, cs.in_timestamp, cs.out_timestamp, cs.car_number " +
                "    from car_state cs " +
                "    where cs.out_timestamp between :dateFrom and :dateTo " +
                "      and cs.out_gate is not null " +
                ") cs on cs.car_number = l.plate_number and cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)");

        List<Object> freeRecordsList = entityManager.createNativeQuery(freeRecordsQueryString.toString())
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .setParameter("dateFromException", dateFromException.getTime())
                .setParameter("dateToException", dateToException.getTime())
                .getResultList();

        fieldsMap.put("freeRecords", freeRecordsList);

        autoClosedRecordsQueryString.append("from car_state cs " +
                "where cs.in_timestamp between :dateFrom and :dateTo " +
                "and cs.out_timestamp is null");

        List<Object> autoClosedRecordsList = entityManager.createNativeQuery(autoClosedRecordsQueryString.toString())
                .setParameter("dateFrom", fromDate)
                .setParameter("dateTo", toDate)
                .getResultList();

        fieldsMap.put("autoClosedRecords", autoClosedRecordsList);

        return fieldsMap;
    }
}
