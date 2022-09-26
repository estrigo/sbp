package kz.spt.app.rest;

import kz.spt.lib.model.dto.dashboard.DashboardOccupancyDto;
import kz.spt.lib.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/rest/dashboard")
public class DashboardRestController {

    private DashboardService dashboardService;

    public DashboardRestController(DashboardService dashboardService){
        this.dashboardService = dashboardService;
    }

    @RequestMapping(value = "/count/freePercentageByTotal", method = RequestMethod.GET)
    public DashboardOccupancyDto freePercentageByTotal() {
        return dashboardService.freePercentageByTotal();
    }

    @RequestMapping(value = "/count/incomeByProviders", method = RequestMethod.POST, consumes = "multipart/form-data")
    public List incomeByProviders(@RequestParam("type") String type, @RequestParam("from") String from, @RequestParam("to") String to) {
        return dashboardService.incomeByProviders(type, from, to);
    }

    @RequestMapping(value = "/count/countPaymentsByProviders", method = RequestMethod.POST, consumes = "multipart/form-data")
    public List countPaymentsByProviders(@RequestParam("type") String type, @RequestParam("from") String from, @RequestParam("to") String to) {
        return dashboardService.countPaymentsByProviders(type, from, to);
    }

    @RequestMapping(value = "/count/countOccupancyInPeriod", method = RequestMethod.POST, consumes = "multipart/form-data")
    public List getOccupancyInPeriod(@RequestParam("type") String type, @RequestParam("from") String from, @RequestParam("to") String to) {
        return dashboardService.occupancyInPeriod(type, from, to);
    }

    @RequestMapping(value = "/count/passByGatesInPeriod", method = RequestMethod.POST, consumes = "multipart/form-data")
    public List passByGatesInPeriod(@RequestParam("type") String type, @RequestParam("from") String from, @RequestParam("to") String to) {
        return dashboardService.passByGatesInPeriod(type, from, to);
    }

    @RequestMapping(value = "/count/durationsInPeriod", method = RequestMethod.POST, consumes = "multipart/form-data")
    public List durationsInPeriod(@RequestParam("type") String type, @RequestParam("from") String from, @RequestParam("to") String to) {
        return dashboardService.durationsInPeriod(type, from, to);
    }

    @RequestMapping(value = "/count/occupancyAndMoneyByPeriod", method = RequestMethod.POST, consumes = "multipart/form-data")
    public Map occupancyAndMoneyByPeriod(@RequestParam("type") String type, @RequestParam("from") String from, @RequestParam("to") String to) {
        return dashboardService.occupancyAndMoneyByPeriod(type, from, to);
    }

    @RequestMapping(value = "/count/realTimeOccupancy", method = RequestMethod.GET)
    public Map realTimeOccupancy() {
        return dashboardService.realTimeOccupancy();
    }
}
