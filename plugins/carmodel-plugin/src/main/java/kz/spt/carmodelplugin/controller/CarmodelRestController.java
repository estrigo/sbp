package kz.spt.carmodelplugin.controller;

import kz.spt.carmodelplugin.service.CarmodelService;
import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Cars;
import kz.spt.lib.service.CarsService;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log
@RestController
@RequestMapping(value = "/rest")
public class CarmodelRestController {

    private CarmodelService carmodelService;

    public CarmodelRestController(CarmodelService carmodelService) {
        this.carmodelService = carmodelService;
    }

    @PostMapping
    public Page<CarmodelDto> list(@RequestBody PagingRequest pagingRequest) throws ParseException {
        CarmodelDto filter = pagingRequest.convertTo(CarmodelDto.builder().build());
//        return carsService.getCars(pagingRequest);
        return null;
    }


    @PostMapping("/carmodel/list")
    public Page<CarmodelDto> getCarmodelDtoList (@RequestBody PagingRequest pagingRequest, @RequestParam String dateFromString,
                                                 @RequestParam String plateNumber) {
//        CarmodelDto filter = pagingRequest.convertTo(CarmodelDto.builder().build());
        CarmodelDto filter = new CarmodelDto();
        filter.setPlateNumber(plateNumber);
        filter.setDateFromString(dateFromString);
        Page<CarmodelDto> resultList;
        resultList = carmodelService.listCarsBy(pagingRequest, filter);
        return resultList;
    }




}
