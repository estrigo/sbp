package kz.spt.carmodelplugin.repository;


import kz.spt.carmodelplugin.viewmodel.CarmodelDto;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository("carmodelRepository")
public interface CarmodelRepository {

    List<Map<String, Object>> getAllCarsByFilter(CarmodelDto filter);

    Long countCarsByFilter(CarmodelDto filter);

}
