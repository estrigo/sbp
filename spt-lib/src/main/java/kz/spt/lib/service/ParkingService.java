package kz.spt.lib.service;

import kz.spt.lib.model.Parking;

public interface ParkingService {

    Iterable<Parking> listAllParking();

    Parking saveParking(Parking parking);

    Parking findById(Long id);
}
