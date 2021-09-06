package kz.spt.rateplugin.repository;

import kz.spt.api.model.Parking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingRepository  extends JpaRepository<Parking, Long> {

    @Query("from Parking p where p.parkingType = 'PAYMENT' or p.parkingType = 'WHITELIST_PAYMENT'")
    List<Parking> listPaymentParkings();
}
