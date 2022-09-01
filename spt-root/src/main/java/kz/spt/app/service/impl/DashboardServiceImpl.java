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
    public String test(String period, String from, String to) {
        LocalDateTime current = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        Date toDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());
        Date fromDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());

        if("day".equals(period)){
            current = current.minusDays(1);
            fromDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());
        } else if("week".equals(period)){
            current = current.minusWeeks(1);
            fromDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());
        } else if("months".equals(period)){
            current = current.minusMonths(1);
            fromDate = Date.from(current.atZone(ZoneId.systemDefault()).toInstant());
        } else if("period".equals(period)){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            fromDate = Date.from(LocalDateTime.parse(from, formatter).truncatedTo(ChronoUnit.HOURS).atZone(ZoneId.systemDefault()).toInstant());
            toDate = Date.from(LocalDateTime.parse(to, formatter).truncatedTo(ChronoUnit.HOURS).atZone(ZoneId.systemDefault()).toInstant());
        }

        if("months".equals(period)){

        }

        return null;
    }
}
