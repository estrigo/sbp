package kz.spt.lib.model.dto;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Parking;
import lombok.Data;

import java.util.List;

@Data
public class ParkingCarsDTO {

    public Parking parking;
    public List<String> carsList;
    public Integer size = 0;
}
