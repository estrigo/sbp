package kz.spt.lib.service;

import kz.spt.lib.model.Parking;
import kz.spt.lib.model.dto.ParkingCarsDTO;
import kz.spt.lib.model.dto.ParkingDto;

import java.util.List;

public interface ParkingService {

    Iterable<Parking> listAllParking();

    Parking saveParking(Parking parking);

    Parking findById(Long id);

    Parking findByType(Parking.ParkingType type);

    void deleteById(Long id);

    List<ParkingCarsDTO> listAllParkingCars();

    ParkingCarsDTO carsInParking(Long parkingId);

    Iterable<Parking> listWhitelistParkings();

    Iterable<Parking> listPaymentParkings();

    List<ParkingDto> getParkings();

    Boolean isLcd();
}
