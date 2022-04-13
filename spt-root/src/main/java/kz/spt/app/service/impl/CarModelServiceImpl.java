package kz.spt.app.service.impl;

import kz.spt.app.repository.CarModelRepository;
import kz.spt.app.repository.CarsRepository;
import kz.spt.lib.model.CarModel;
import kz.spt.lib.model.Cars;
import kz.spt.lib.service.CarModelService;
import kz.spt.lib.service.EventLogService;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

@Log
@Service
public class CarModelServiceImpl implements CarModelService {
    private CarModelRepository carModelRepository;
    private EventLogService eventLogService;

    public CarModelServiceImpl(CarModelRepository carModelRepository){
        this.carModelRepository = carModelRepository;
    }

    @Override
    public CarModel getByModel(String model){
        return carModelRepository.getByModel(model);
    }
}
