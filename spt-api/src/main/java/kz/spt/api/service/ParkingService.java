package kz.spt.api.service;

import kz.spt.api.model.Parking;

public interface ParkingService {

    Iterable<Parking> listAllParking();

    Parking saveParking(Parking parking);

    Parking findById(Long id);
}