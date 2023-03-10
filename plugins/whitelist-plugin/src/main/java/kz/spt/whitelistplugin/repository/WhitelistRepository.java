package kz.spt.whitelistplugin.repository;

import kz.spt.lib.model.Cars;
import kz.spt.whitelistplugin.model.Whitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository("whitelistRepository")
public interface WhitelistRepository extends JpaRepository<Whitelist, Long> {

    @Query("from Whitelist w where w.car = ?1 and w.parking.id = ?3 and w.group is null and (w.type = 'UNLIMITED' or w.type = 'CUSTOM' or (w.type = 'PERIOD' and ?2 between w.access_start and w.access_end) or (w.type = 'PERIOD' and w.access_start is null and ?2 <= w.access_end))")
    List<Whitelist> findValidWhiteListByCar(Cars car, Date date, Long parkingId);

    @Query("from Whitelist w LEFT JOIN FETCH w.group g where w.car = ?1 and g.parking.id = ?3 and w.group is not null and (g.type = 'UNLIMITED' or g.type = 'CUSTOM' or (g.type = 'PERIOD' and ?2 between g.access_start and g.access_end) or (g.type = 'PERIOD' and g.access_start is null and ?2 <= g.access_end))")
    List<Whitelist> findValidWhiteListGroupByCar(Cars car, Date date, Long parkingId);

    @Query("from Whitelist w where w.car = ?1 and w.parking.id = ?4 and w.group is null and (w.type = 'UNLIMITED' or w.type = 'CUSTOM' or (w.type = 'PERIOD' and (?2 between w.access_start and w.access_end or ?3 between w.access_start and w.access_end or (?2 <= w.access_start and ?3 >= w.access_end) or (w.access_start is null and ?2 <= w.access_end))))")
    List<Whitelist> findValidWhiteListByCar(Cars car, Date inDate, Date outDate, Long parkingId);

    @Query("from Whitelist w LEFT JOIN FETCH w.group g where w.car = ?1 and g.parking.id = ?4 and w.group is not null and (g.type = 'UNLIMITED' or g.type = 'CUSTOM' or (g.type = 'PERIOD' and (?2 between g.access_start and g.access_end or ?3 between g.access_start and g.access_end or (?2 <= g.access_start and ?3 >= g.access_end) or (g.access_start is null and ?2 <= g.access_end))))")
    List<Whitelist> findValidWhiteListGroupByCar(Cars car, Date inDate, Date outDate, Long parkingId);

    @Query("from Whitelist w LEFT JOIN FETCH w.group g where w.car = ?1 and g is not null and g.parking.id = ?3 and g.type = 'BOTTAXI' and g.access_end is not null and g.access_start is not null and ?2 between g.access_start and g.access_end")
    List<Whitelist> findWhitelistGroupTaxiByCar(Cars car, Date date, Long parkingId);

    @Query("from Whitelist w LEFT JOIN FETCH w.group g where w.car = ?1 and g is not null and g.parking.id = ?4 and g.type = 'BOTTAXI' and g.access_end is not null and g.access_start is not null and (?2 between g.access_start and g.access_end or ?3 between g.access_start and g.access_end or (?2 <= g.access_start and ?3 >= g.access_end))")
    List<Whitelist> findWhitelistGroupTaxiByCar(Cars car, Date inDate, Date outDate, Long parkingId);

    @Query("from Whitelist w LEFT JOIN FETCH w.parking LEFT JOIN FETCH w.car LEFT JOIN FETCH w.group LEFT JOIN FETCH w.group.parking where w.id = ?1")
    Whitelist getWithCarAndGroupAndParking(Long id);

    @Query("from Whitelist w where w.group.id = ?1")
    List<Whitelist> findByGroupId(Long groupId);

    @Query("from Whitelist w where w.group.name = ?1")
    List<Whitelist> findByGroupName(String groupName);

    @Query("from Whitelist w where w.car = ?1 and w.parking.id = ?2")
    Whitelist findWhiteListByCar(Cars car, Long parkingId);

    @Query("from Whitelist w where w.car.platenumber = ?1 and w.parking.id = ?2")
    Whitelist findByPlatenumber(String platenumber, Long parkingId);

    @Query("SELECT w.car.platenumber from Whitelist w where w.car.platenumber in (?1) and w.parking.id = ?2")
    List<String> getExistingPlatenumbers(List<String> platenumbers, Long parkingId);

    @Query("SELECT w.car.platenumber from Whitelist w where w.car.platenumber in (?1) and w.parking.id = ?2 and (w.group is null or w.group.id <> ?3)")
    List<String> getExistingPlatenumbers(List<String> platenumbers, Long parkingId, Long groupId);

    @Query("from Whitelist w where w.parking.id = ?1")
    List<Whitelist> findByParkingID(Long parkingId);
}
