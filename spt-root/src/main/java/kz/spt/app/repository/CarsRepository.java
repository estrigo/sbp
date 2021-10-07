package kz.spt.app.repository;

import kz.spt.lib.model.Cars;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CarsRepository extends JpaRepository<Cars, Long> {

    Cars findCarsByPlatenumberIgnoreCase(String platenumber);

    @Query("from Cars c LEFT JOIN FETCH c.customer WHERE c.platenumber = ?1")
    Cars findCarsByPlatenumberWithCustomers(String platenumber);

    Iterable<Cars> findCarsByDeletedFalse();
}
