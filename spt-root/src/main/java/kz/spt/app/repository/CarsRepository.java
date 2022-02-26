package kz.spt.app.repository;

import kz.spt.lib.model.Cars;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarsRepository extends JpaRepository<Cars, Long>, JpaSpecificationExecutor<Cars> {

    Cars findCarsByPlatenumberIgnoreCase(String platenumber);

    @Query("from Cars c LEFT JOIN FETCH c.customer WHERE c.platenumber = ?1")
    Cars findCarsByPlatenumberWithCustomers(String platenumber);

    @Query("from Cars c where c.platenumber like %:platenumber%")
    List<Cars> findByPlatenumberContaining(String platenumber);

    Iterable<Cars> findCarsByDeletedFalse();

    @Query("select c.platenumber from Cars c where c.platenumber like %:text% order by c.id desc")
    List<String> searchPlateNumbersIgnoreCase(String text, Pageable page);
}
