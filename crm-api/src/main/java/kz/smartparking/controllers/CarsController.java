package kz.smartparking.controllers;


import kz.smartparking.service.CarsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cars")
public class CarsController {

    private CarsService carsService;

    public CarsController(CarsService carsService){
        this.carsService = carsService;
    }
}
