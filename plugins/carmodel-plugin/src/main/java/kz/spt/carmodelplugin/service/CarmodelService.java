package kz.spt.carmodelplugin.service;

import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.CarModel;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface CarmodelService {

    Page<CarmodelDto> listCarsBy(PagingRequest pagingRequest, CarmodelDto filter);

    void editDimensionOfCar(String plateNumber, String dimension);

    List<CarModel> findAll();

    CarModel getCarModelById(Integer id);

    void deleteCarModel(Integer id);

    org.springframework.data.domain.Page<CarModel> findAllUsersPageable(Pageable pageable);

    CarModel findByModel(String model);

    void saveCarModel(CarModel carModel, UserDetails currentUser);

    void updateCarModel(int id, CarModel carModel, UserDetails currentUser);

    }
