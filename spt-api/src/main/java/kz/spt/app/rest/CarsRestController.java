package kz.spt.app.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Cars;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.CarEventService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(value = "/rest/cars")
public class CarsRestController {

    private CarEventService carEventService;
    private CarsService carsService;

    public CarsRestController(CarEventService carEventService, CarsService carsService){
        this.carEventService = carEventService;
        this.carsService = carsService;
    }

    @RequestMapping(value = "/event", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @Transactional
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addCarEvent(@Valid @RequestBody CarEventDto carEventDto) throws Exception{
        carEventService.saveCarEvent(carEventDto);
    }

    @PostMapping
    public Page<Cars> list(@RequestBody PagingRequest pagingRequest) {
        return carsService.getCars(pagingRequest);
    }
}
