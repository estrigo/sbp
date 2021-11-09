package kz.spt.app.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarStateDto;
import kz.spt.lib.service.CarStateService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/rest/carstate")
public class CarStateRestController {

    private CarStateService carStateService;

    public CarStateRestController(CarStateService carStateService){
        this.carStateService = carStateService;
    };

    @PostMapping
    public Page<CarStateDto> list(@RequestBody PagingRequest pagingRequest, @RequestParam String plateNumber,
                                  @RequestParam String dateFromString, @RequestParam String dateToString){
        return carStateService.getAll(pagingRequest, plateNumber, dateFromString, dateToString);
    }
}