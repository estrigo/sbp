package kz.spt.api.service;

import kz.spt.api.bootstrap.datatable.Page;
import kz.spt.api.bootstrap.datatable.PagingRequest;
import kz.spt.api.model.Cars;

public interface CarsService {

    Cars findByPlatenumber(String platenumber);

    Cars findById(Long id);

    Iterable<Cars> listAllCars();

    Cars saveCars(Cars cars);

    Iterable<Cars> findAllByDeletedFalse();

    void createCar(String platenumber);

    Page<Cars> getCars(PagingRequest pagingRequest);
}
