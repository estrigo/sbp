package kz.spt.whitelistplugin.repository;


import kz.spt.lib.model.Parking;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("whitelistGroupsRepository")
public interface WhitelistGroupsRepository extends JpaRepository<WhitelistGroups, Long> {

    @Query("from WhitelistGroups w  where w.id = ?1")
    WhitelistGroups getWhitelistGroup(Long id);

    @Query("from WhitelistGroups w where w.parking.id = ?1")
    List<WhitelistGroups> getWhitelistGroupByParkingId(Long parkingId);

    WhitelistGroups getWhitelistGroupsByName(String groupName);

    WhitelistGroups getWhitelistGroupsByNameAndParking(String groupName, Parking parking);

}
