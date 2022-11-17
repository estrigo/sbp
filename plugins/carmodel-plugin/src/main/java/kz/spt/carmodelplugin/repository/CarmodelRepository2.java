package kz.spt.carmodelplugin.repository;

import kz.spt.lib.model.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CarmodelRepository2  extends JpaRepository<CarModel, Integer> {

    @Query(nativeQuery = true, value = "SELECT * FROM car_model c WHERE c.id = :id")
    CarModel getById(Integer id);

    CarModel findByModel (String model);
}
