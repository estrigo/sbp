package kz.spt.lib.service;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Cars;

import java.util.List;

public interface CarsService {

    Cars findByPlatenumber(String platenumber);

    List<Cars> findByPlatenumberContaining(String platenumber);

    Cars findByPlatenumberWithCustomer(String platenumber);

    Cars findById(Long id);

    Iterable<Cars> listAllCars();

    Cars saveCars(Cars cars);

    Cars createCar(String platenumber, String region, String type, String car_model);

    Cars createCar(String platenumber);

    Page<Cars> getCars(PagingRequest pagingRequest);

    List<String> searchByPlateNumberContaining(String text);
}
