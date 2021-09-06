package kz.spt.rateplugin.repository;

import kz.spt.rateplugin.model.ParkingRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RateRepository extends JpaRepository<ParkingRate, Long> {

    @Query("from ParkingRate pr JOIN FETCH pr.parking where pr.id = ?1")
    ParkingRate getWithParking(Long id);
}