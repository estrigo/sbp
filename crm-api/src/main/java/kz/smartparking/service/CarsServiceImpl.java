package kz.smartparking.service;

import kz.smartparking.model.Cars;
import kz.smartparking.repository.CarsRepository;
import org.springframework.stereotype.Service;

@Service
public class CarsServiceImpl implements CarsService {

    private CarsRepository carsRepository;

    public Cars findByNumberplate(String numberplate){
        return carsRepository.getOne(numberplate);
    }
}
