package kz.spt.app.service.impl;

import kz.spt.api.model.Camera;
import kz.spt.api.model.Cars;
import kz.spt.api.service.EventLogService;
import kz.spt.app.repository.CarsRepository;
import kz.spt.api.service.CarsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CarsServiceImpl implements CarsService {

    @Autowired
    private CarsRepository carsRepository;

    @Autowired
    private EventLogService eventLogService;

    public Cars findByPlatenumber(String platenumber){
        return carsRepository.findCarsByPlatenumberIgnoreCase(platenumber);
    }

    public Cars findById(Long id){
        return carsRepository.getOne(id);
    }

    public Iterable<Cars> listAllCars(){
        return carsRepository.findAll();
    }

    public Cars saveCars(Cars cars){
        cars.setPlatenumber(cars.getPlatenumber().toUpperCase());
        return carsRepository.save(cars);
    }

    public void createCar(String platenumber){
        if(findByPlatenumber(platenumber) == null){
            Cars cars = new Cars();
            cars.setPlatenumber(platenumber);
            Cars savedCar = saveCars(cars);
            eventLogService.createEventLog(String.valueOf(Cars.class), savedCar.getId(), null, "Новый номер авто " + cars.getPlatenumber() + " сохранен в системе ");
        }
    }

    public Iterable<Cars> findAllByDeletedFalse(){
        return carsRepository.findCarsByDeletedFalse();
    }
}
