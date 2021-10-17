package kz.spt.lib.service;

import kz.spt.lib.model.Parking;
import kz.spt.lib.model.dto.ParkingCarsDTO;

import java.util.List;

public interface ParkingService {

    Iterable<Parking> listAllParking();

    Parking saveParking(Parking parking);

    Parking findById(Long id);

    void deleteById(Long id);

    List<ParkingCarsDTO> listAllParkingCars();

    ParkingCarsDTO carsInParking(Long parkingId);

    Iterable<Parking> listWhitelistParkings();
}
