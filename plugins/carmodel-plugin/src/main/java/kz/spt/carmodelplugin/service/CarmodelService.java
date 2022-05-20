package kz.spt.carmodelplugin.service;

import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;

import java.text.ParseException;
import java.util.List;

public interface CarmodelService {

    Page<CarmodelDto> listCarsBy(PagingRequest pagingRequest, CarmodelDto filter);

    void editDimensionOfCar(String plateNumber, String dimension);
}
