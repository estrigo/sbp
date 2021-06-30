package kz.smartparking.service;

import kz.smartparking.model.Cars;

public interface CarsService {

    Cars findByNumberplate(String numberplate);

    Cars findById(Long id);

    void saveCars(Cars cars);
}
