package kz.spt.whitelistplugin.model.dto;

import kz.spt.api.model.Cars;
import kz.spt.api.model.Parking;
import lombok.Data;

import java.util.List;

@Data
public class ParkingCarsDTO {

    public Parking parking;
    public List<Cars> carsList;

}
