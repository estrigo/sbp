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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%m')");
            } else if("month".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%d')");
            } else if("week".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%a')");
            } else if("day".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%H')");
            } else if("period".equals(period)){
                queryString =  queryString.replace("p.created >= :fromDate", "p.created >= :fromDate and p.created  <= :toDate");
                Long diff = toDate.getTime() - fromDate.getTime();
                long days = diff / (1000*60*60*24);
                if(days > 1d){
                    if(days > 7d){
                        if(days > 31d){
                            queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%m')");
                        } else {
                            queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%d')");
                        }
                    } else {
                        queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%a')");
                    }
                } else {
                    queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%H')");
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
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%m')");
            } else if("month".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%d')");
            } else if("week".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%a')");
            } else if("day".equals(period)){
                queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%H')");
            } else if("period".equals(period)){
                queryString =  queryString.replace("p.created >= :fromDate", "p.created >= :fromDate and p.created  <= :toDate");
                Long diff = toDate.getTime() - fromDate.getTime();
                long days = diff / (1000*60*60*24);
                if(days > 1d){
                    if(days > 7d){
                        if(days > 31d){
                            queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%m')");
                        } else {
                            queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%d')");
                        }
                    } else {
                        queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%a')");
                    }
                } else {
                    queryString  = queryString.replaceAll("PERIOD", "DATE_FORMAT(" + (timezoneShift >= 0 ? "date_add" : "date_subtract") + "(p.created, INTERVAL " + Math.abs(timezoneShift) + " hour), '%H')");
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

        if(timezoneShift > 0){
            current = current.plusHours(timezoneShift);
            till = till.plusHours(timezoneShift);
        } else if(timezoneShift < 0){
            current = current.minusDays(timezoneShift);
            till = till.minusDays(timezoneShift);
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

                sum.append(" sum(case when (cs.in_timestamp between '" + current.format(formatter) + "' and " + current.plusHours(1).format(formatter) + ") or (cs.out_timestamp between '" + current.format(formatter) + "' and '" + current.plusHours(1).format(formatter) + "') or (cs.in_timestamp <= '" + current.format(formatter) + "' and cs.out_timestamp >= '" + current.plusHours(1).format(formatter) + "') then 1 else 0 end) as '" + current.getHour() + "'");
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
                " group by g.id, g.name;";

        List entryResult =  entityManager.createNativeQuery(entryQueryString).setParameter("fromDate", fromDate).setParameter("toDate", toDate).getResultList();
        List exitResult =  entityManager.createNativeQuery(exitQueryString).setParameter("fromDate", fromDate).setParameter("toDate", toDate).getResultList();

        entryResult.addAll(exitResult);
        return entryResult;
    }

    @Override
    public List durationsInPeriod(String period, String from, String to) {
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

        String durationQueryString = "select sum(case when source.diff < 1 then 1 else 0 end) as '0-1', " +
                "       sum(case when source.diff >= 1 and source.diff < 2 then 1 else 0 end) as '1-2', " +
                "       sum(case when source.diff >= 2 and source.diff < 3 then 1 else 0 end) as '2-3', " +
                "       sum(case when source.diff >= 3 and source.diff < 4 then 1 else 0 end) as '3-4', " +
                "       sum(case when source.diff >= 4 then 1 else 0 end) as '4' " +
                "from (select TIMESTAMPDIFF(hour, cs.in_timestamp, coalesce(cs.out_timestamp, now())) as diff " +
                "      from car_state cs " +
                "      where (:fromDate between cs.in_timestamp and cs.out_timestamp) " +
                "         or (:toDate between cs.in_timestamp and cs.out_timestamp) " +
                "         or (:toDate <= cs.in_timestamp and cs.out_timestamp is null) " +
                "         or (:fromDate <= cs.in_timestamp and :toDate >= cs.out_timestamp) " +
                ") as source";

        List durationResult =  entityManager.createNativeQuery(durationQueryString).setParameter("fromDate", fromDate).setParameter("toDate", toDate).getResultList();

        return durationResult;
    }
}
