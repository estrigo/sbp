package kz.spt.lib.service;

import kz.spt.lib.model.dto.dashboard.DashboardOccupancyDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface DashboardService {

    DashboardOccupancyDto freePercentageByTotal();

    List incomeByProviders(String period, String from, String to);

    List countPaymentsByProviders(String period, String from, String to);

    List occupancyInPeriod(String period, String from, String to);

    Map passByGatesInPeriod(String period, String from, String to);

    List durationsInPeriod(String period, String from, String to);

    Map occupancyAndMoneyByPeriod(String period, String from, String to);

    Map realTimeOccupancy();

    Boolean dashboardEnabled();
}
