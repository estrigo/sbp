package kz.spt.app.service.impl;

import kz.spt.app.entity.dto.CarEventDto;
import kz.spt.app.service.CarEventService;
import kz.spt.api.service.CarsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CarEventServiceImpl implements CarEventService {

    @Autowired
    private CarsService carsService;

    @Override
    public void saveCarEvent(CarEventDto eventDto) {
        carsService.createCar(eventDto.car_number);
    }
}
