package crm.service.impl;

import crm.repository.CarsRepository;
import crm.model.Cars;
import crm.service.CarsService;
import org.springframework.stereotype.Service;

@Service
public class CarsServiceImpl implements CarsService {

    private CarsRepository carsRepository;

    public CarsServiceImpl(CarsRepository carsRepository){
        this.carsRepository = carsRepository;
    }

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
