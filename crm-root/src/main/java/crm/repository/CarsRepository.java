package crm.repository;

import crm.model.Cars;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarsRepository extends JpaRepository<Cars, Long> {

    Cars findCarsByPlatenumberIgnoreCase(String platenumber);

    Iterable<Cars> findCarsByDeletedFalse();
}
