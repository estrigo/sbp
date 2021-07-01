package crm.service;

import crm.repository.CarsRepository;
import crm.model.Cars;
import org.springframework.stereotype.Service;

@Service
public class CarsServiceImpl implements CarsService {

    private CarsRepository carsRepository;

    public CarsServiceImpl(CarsRepository carsRepository){
        this.carsRepository = carsRepository;
    }

    public Cars findByPlatenumber(String platenumber){
        return carsRepository.findCarsByPlatenumber(platenumber);
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

    public Iterable<Cars> findAllByDeletedFalse(){
        return carsRepository.findCarsByDeletedFalse();
    }
}
