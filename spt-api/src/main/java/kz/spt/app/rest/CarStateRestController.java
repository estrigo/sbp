package kz.spt.app.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.dto.CarStateDto;
import kz.spt.lib.model.dto.CarStateExcelDto;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.CarStateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

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
        return carStateService.removeDebt(plateNumber, true);
    }


    @GetMapping("/excel")
    public List<CarStateExcelDto> excellist(@RequestParam String dateFromString, @RequestParam String dateToString,
                                            @RequestParam String plateNumber, @RequestParam Long inGateId,
                                            @RequestParam Long outGateId) throws ParseException {
        CarStateFilterDto filter = new CarStateFilterDto();
        filter.setDateFromString(dateFromString);
        filter.setDateToString(dateToString);
        filter.setPlateNumber(plateNumber);
        filter.setInGateId(inGateId);
        filter.setOutGateId(outGateId);
        return carStateService.getExcelData(filter);
    }

    @GetMapping("/cars/in")
    public List<String> getAllCarsInParking()
    {
        return carStateService.getCarsInParking();
    }

    @GetMapping("/cars/in/paid")
    public List<String> getNotPaidCarsInParking()
    {
        return carStateService.getCarsInParkingAndNotPaid();
    }

}
