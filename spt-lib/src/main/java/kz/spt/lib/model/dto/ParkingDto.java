package kz.spt.lib.model.dto;

import kz.spt.lib.model.Parking;

import java.util.ArrayList;
import java.util.List;

public class ParkingDto {

    public Long id;
    public String name;
    public String description;
    public Parking.ParkingType parkingType;

    public static ParkingDto fromParking(Parking parking){
        ParkingDto dto = new ParkingDto();
        dto.id = parking.getId();
        dto.name = parking.getName();
        dto.description  = parking.getDescription();
        dto.parkingType = parking.getParkingType();
        return dto;
    }

    public static List<ParkingDto>fromParking(List<Parking> parkings){
        List<ParkingDto> parkingDtos = new ArrayList<>();
        for(Parking parking : parkings){
            parkingDtos.add(fromParking(parking));
        }
        return parkingDtos;
    }
}
