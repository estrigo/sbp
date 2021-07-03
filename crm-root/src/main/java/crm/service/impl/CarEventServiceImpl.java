package crm.service.impl;

import crm.entity.dto.CarEventDto;
import crm.service.CarEventService;
import crm.service.CarsService;
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
