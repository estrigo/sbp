package kz.spt.whitelistplugin.service;

import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.model.dto.ParkingCarsDTO;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;

public interface WhitelistService {

    void saveWhitelist(Whitelist whitelist, UserDetails currentUser) throws Exception;

    Iterable<Whitelist> listAllWhitelist();

    Boolean hasAccess(String plateNumber, Date enterDate);

    Whitelist prepareById(Long id);

    List<ParkingCarsDTO> listAllCarsInParking();

    ParkingCarsDTO carsInParking(Long parkingId);

    void deleteById(Long id);

    void saveWhitelistFromGroup(String plateNumber, WhitelistGroups group, String currentUser);
}