package kz.spt.whitelistplugin.model.dto;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Parking;
import lombok.Data;

import java.util.List;

@Data
public class ParkingCarsDTO {

    public Parking parking;
    public List<Cars> carsList;

}
