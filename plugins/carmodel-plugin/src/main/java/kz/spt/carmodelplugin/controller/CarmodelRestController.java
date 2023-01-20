package kz.spt.carmodelplugin.controller;

import kz.spt.carmodelplugin.service.CarDimensionsService;
import kz.spt.carmodelplugin.service.CarModelServicePl;
import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Dimensions;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@Log
@RestController
@RequestMapping(value = "/rest")
public class CarmodelRestController {

    private CarModelServicePl carModelServicePl;
    private CarDimensionsService carDimensionsService;

    public CarmodelRestController(CarModelServicePl carModelServicePl, CarDimensionsService carDimensionsService) {
        this.carModelServicePl = carModelServicePl;
        this.carDimensionsService = carDimensionsService;
    }

    @PostMapping
    public Page<CarmodelDto> list(@RequestBody PagingRequest pagingRequest) throws ParseException {
        CarmodelDto filter = pagingRequest.convertTo(CarmodelDto.builder().build());
//        return carsService.getCars(pagingRequest);
        return null;
    }

    @GetMapping("/carmodel/getDimensions")
    public List<Dimensions> getCarmodelDtoList () {
        return carDimensionsService.listDimensions();
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
