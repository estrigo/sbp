package kz.spt.app.service.impl;

import kz.spt.api.model.Parking;
import kz.spt.app.repository.ParkingRepository;
import kz.spt.api.service.ParkingService;
import org.springframework.stereotype.Service;

@Service
public class ParkingServiceImpl implements ParkingService {

    private ParkingRepository parkingRepository;

    public ParkingServiceImpl(ParkingRepository parkingRepository){
        this.parkingRepository = parkingRepository;
    }

    @Override
    public Iterable<Parking> listAllParking() {
        return parkingRepository.findAll();
    }

    @Override
    public Parking saveParking(Parking parking) {
        return parkingRepository.save(parking);
    }

    @Override
    public Parking findById(Long id) {
        return parkingRepository.getOne(id);
    }
}
