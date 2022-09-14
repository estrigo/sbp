package kz.spt.app.rest;

import kz.spt.lib.model.dto.dashboard.DashboardOccupancyDto;
import kz.spt.lib.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
