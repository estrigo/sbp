package kz.spt.whitelistplugin.service.impl;

import kz.spt.api.model.Cars;
import kz.spt.api.service.CarsService;
import kz.spt.whitelistplugin.repository.CarsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CarsServiceImpl implements CarsService {

    @Autowired
    private CarsRepository carsRepository;

    public Cars findByPlatenumber(String platenumber){
        return carsRepository.findCarsByPlatenumberIgnoreCase(platenumber);
    }

    public Cars findById(Long id){
        return carsRepository.getOne(id);
    }

    public Iterable<Cars> listAllCars(){
        return carsRepository.findAll();
    }

    public void saveCars(Cars cars){
        if(cars.getPlatenumber()!=null){
            cars.setPlatenumber(cars.getPlatenumber().toUpperCase());
            carsRepository.save(cars);
        }
    }

    public void createCar(String platenumber){
        if(findByPlatenumber(platenumber) == null){
            Cars cars = new Cars();
            cars.setPlatenumber(platenumber);
            saveCars(cars);
        }
    }

    public Iterable<Cars> findAllByDeletedFalse(){
        return carsRepository.findCarsByDeletedFalse();
    }
}
