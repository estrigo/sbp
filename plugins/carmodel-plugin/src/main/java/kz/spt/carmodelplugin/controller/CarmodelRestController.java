package kz.spt.carmodelplugin.controller;

import kz.spt.carmodelplugin.service.CarModelServicePl;
import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@Log
@RestController
@RequestMapping(value = "/rest")
public class CarmodelRestController {

    private CarModelServicePl carModelServicePl;

    public CarmodelRestController(CarModelServicePl carModelServicePl) {
        this.carModelServicePl = carModelServicePl;
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
        resultList = carModelServicePl.listCarsBy(pagingRequest, filter);
        return resultList;
    }




}
