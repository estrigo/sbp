package kz.spt.app.repository;

import kz.spt.lib.model.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CarModelRepository extends JpaRepository<CarModel, Integer> {

    @Query(nativeQuery = true, value = "SELECT * FROM car_model c WHERE c.model = :model ORDER BY c.model DESC LIMIT 1")
    CarModel getByModel(String model);
}
