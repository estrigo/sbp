package kz.spt.whitelistplugin.service;

import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.dto.ParkingCarsDTO;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;

public interface WhitelistService {

    void saveWhitelist(Whitelist whitelist, UserDetails currentUser) throws Exception;

    Iterable<Whitelist> listAllWhitelist();

    Boolean hasAccess(String platenumber, Date enterDate);

    Whitelist findById(Long id);

    List<ParkingCarsDTO> listAllCarsInParking();

    ParkingCarsDTO carsInParking(Long parkingId);

}