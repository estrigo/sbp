package kz.smartparking.service;

import kz.smartparking.model.Cars;

public interface CarsService {

    Cars findByNumberplate(String numberplate);

}
