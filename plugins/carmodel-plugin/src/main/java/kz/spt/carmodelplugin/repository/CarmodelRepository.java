package kz.spt.carmodelplugin.repository;


import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import kz.spt.lib.model.Cars;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository("carmodelRepository")
public interface CarmodelRepository {

    public List<Map<String, String>>  getAll();

    List<Map<String, Object>> getAllCarsByFilter(CarmodelDto filter);

    Long countCarsByFilter(CarmodelDto filter);

}
