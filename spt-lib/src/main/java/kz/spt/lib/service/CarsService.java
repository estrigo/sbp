package kz.spt.lib.service;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Cars;

public interface CarsService {

    Cars findByPlatenumber(String platenumber);

    Cars findByPlatenumberWithCustomer(String platenumber);

    Cars findById(Long id);

    Iterable<Cars> listAllCars();

    Cars saveCars(Cars cars);

    Iterable<Cars> findAllByDeletedFalse();

    Cars createCar(String platenumber);

    Page<Cars> getCars(PagingRequest pagingRequest);
}
