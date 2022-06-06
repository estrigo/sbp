package kz.spt.app.repository;

import kz.spt.lib.model.Parking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

@Repository
public interface ParkingRepository extends JpaRepository<Parking, Long> {

    @Query("from Parking p where p.parkingType = 'WHITELIST' or p.parkingType = 'WHITELIST_PAYMENT' or p.parkingType = 'PREPAID'")
    List<Parking> whitelistParkings();

    List<Parking> findParkingByParkingType(Parking.ParkingType parkingType);

    @Query("from Parking p where p.parkingType = 'PAYMENT' or p.parkingType = 'WHITELIST_PAYMENT'")
    List<Parking> paymentParkings();
}
