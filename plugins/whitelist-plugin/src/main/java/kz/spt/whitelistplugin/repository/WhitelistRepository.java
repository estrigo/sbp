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

    @Query("from Whitelist w where w.kind = 'INDIVIDUAL' and w.car = ?1 and (w.type = 'UNLIMITED' or ((w.type = 'PERIOD' or w.type = 'MONTHLY' or w.type = 'ONCE') and ?2 between w.access_start and w.access_end))")
    List<Whitelist> findValidWhiteListByCar(Cars car, Date date);

    @Query("from Whitelist w LEFT JOIN FETCH w.group.cars c1 where w.kind = 'GROUP' and c1 = ?1" +
            " and (w.type = 'UNLIMITED' or ((w.type = 'PERIOD' or w.type = 'MONTHLY' or w.type = 'ONCE') and ?2 between w.access_start and w.access_end))")
    List<Whitelist> findValidGroupWhiteListByCar(Cars car, Date date);

    @Query("from Whitelist w LEFT JOIN FETCH w.car LEFT JOIN FETCH w.group.cars where w.id = ?1")
    Whitelist getWithCarAndGroup(Long id);
}
