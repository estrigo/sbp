package kz.spt.app.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarStateDto;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.CarStateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping(value = "/rest/carstate")
public class CarStateRestController {

    private CarStateService carStateService;

    public CarStateRestController(CarStateService carStateService) {
        this.carStateService = carStateService;
    }

    @PostMapping
    public Page<CarStateDto> list(@RequestBody PagingRequest pagingRequest) throws ParseException {
        CarStateFilterDto filter = pagingRequest.convertTo(CarStateFilterDto.builder().build());
        return carStateService.getAll(pagingRequest, filter);
    }

    @PreAuthorize("hasAnyRole('ROLE_OPERATOR_NO_REVENUE_SHARE')")
    @GetMapping("/remove/debt")
    public Boolean removeDebt(@RequestParam String plateNumber) throws Exception {
        return carStateService.removeDebt(plateNumber);
    }

    @PostMapping("/entries")
    public Page<CarStateDto> listEntries(@RequestBody PagingRequest pagingRequest,
                                  @RequestParam String plateNumber,
                                  @RequestParam String dateFromString,
                                  @RequestParam String dateToString,
                                  @RequestParam Long inGateId) throws ParseException {
        return carStateService.getEntriesWithoutExit(pagingRequest, CarStateFilterDto.builder()
                .plateNumber(plateNumber)
                .dateFromString(dateFromString)
                .dateToString(dateToString)
                .inGateId(inGateId)
                .build());
    }

    @PostMapping("/exits")
    public Page<CarStateDto> listExits(@RequestBody PagingRequest pagingRequest,
                                  @RequestParam String plateNumber,
                                  @RequestParam String dateFromString,
                                  @RequestParam String dateToString,
                                  @RequestParam Long outGateId) throws ParseException {
        return carStateService.getExitsWithoutEntry(pagingRequest, CarStateFilterDto.builder()
                .plateNumber(plateNumber)
                .dateFromString(dateFromString)
                .dateToString(dateToString)
                .outGateId(outGateId)
                .build());
    }
}
