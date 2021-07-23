package kz.spt.api.service;

import kz.spt.api.model.Cars;

public interface CarsService {

    Cars findByPlatenumber(String platenumber);

    Cars findById(Long id);

    Iterable<Cars> listAllCars();

    Cars saveCars(Cars cars);

    Iterable<Cars> findAllByDeletedFalse();

    void createCar(String platenumber);
}
