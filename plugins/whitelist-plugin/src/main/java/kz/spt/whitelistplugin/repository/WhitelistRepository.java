package kz.spt.whitelistplugin.repository;

import kz.spt.lib.model.Cars;
import kz.spt.whitelistplugin.model.Whitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface WhitelistRepository extends JpaRepository<Whitelist, Long> {

    @Query("from Whitelist w where w.car = ?1 and w.parking.id = ?3 and w.group is null and (w.type = 'UNLIMITED' or w.type = 'CUSTOM' or (w.type = 'PERIOD' and ?2 between w.access_start and w.access_end))")
    List<Whitelist> findValidWhiteListByCar(Cars car, Date date, Long parkingId);

    @Query("from Whitelist w LEFT JOIN FETCH w.group g where w.car = ?1 and g.parking.id = ?3 and w.group is not null and (g.type = 'UNLIMITED' or g.type = 'CUSTOM' or (g.type = 'PERIOD' and ?2 between g.access_start and g.access_end))")
    List<Whitelist> findValidWhiteListGroupByCar(Cars car, Date date, Long parkingId);

    @Query("from Whitelist w LEFT JOIN FETCH w.parking LEFT JOIN FETCH w.car LEFT JOIN FETCH w.group LEFT JOIN FETCH w.group.parking where w.id = ?1")
    Whitelist getWithCarAndGroupAndParking(Long id);

    @Query("from Whitelist w where w.group.id = ?1")
    List<Whitelist> findByGroupId(Long groupId);

    Whitelist findWhiteListByCar(Cars car, Long parkingId);
}
