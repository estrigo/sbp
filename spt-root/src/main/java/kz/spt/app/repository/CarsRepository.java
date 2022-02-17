package kz.spt.app.repository;

import kz.spt.lib.model.Cars;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarsRepository extends JpaRepository<Cars, Long> {

    Cars findCarsByPlatenumberIgnoreCase(String platenumber);

    @Query("from Cars c LEFT JOIN FETCH c.customer WHERE c.platenumber = ?1")
    Cars findCarsByPlatenumberWithCustomers(String platenumber);

    @Query("from Cars c where c.platenumber like %:platenumber%")
    List<Cars> findByPlatenumberContaining(String platenumber);

    Iterable<Cars> findCarsByDeletedFalse();
}
