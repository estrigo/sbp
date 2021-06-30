package kz.smartparking.service;

import kz.smartparking.model.Cars;
import kz.smartparking.repository.CarsRepository;
import org.springframework.stereotype.Service;

@Service
public class CarsServiceImpl implements CarsService {

    private CarsRepository carsRepository;

    public CarsServiceImpl(CarsRepository carsRepository){
        this.carsRepository = carsRepository;
    }

    public Cars findByNumberplate(String numberplate){
        return carsRepository.findCarsByNumberplate(numberplate);
    }

    public Cars findById(Long id){
        return carsRepository.getOne(id);
    }

    public void saveCars(Cars cars){

        if(cars.getNumberplate()!=null){
            cars.setNumberplate(cars.getNumberplate().toUpperCase());
            carsRepository.save(cars);
        }

    }
}
