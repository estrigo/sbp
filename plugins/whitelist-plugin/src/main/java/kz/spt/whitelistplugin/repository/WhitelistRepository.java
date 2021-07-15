package kz.spt.whitelistplugin.repository;

import kz.spt.api.model.Cars;
import kz.spt.whitelistplugin.model.Whitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface WhitelistRepository extends JpaRepository<Whitelist, Long> {

    Iterable<Whitelist> findAllByCarIsNotNull();

    @Query("from Whitelist w where w.car = ?1 and (w.type = 'UNLIMITED' or (w.type = 'PERIOD' and ?2 between w.access_start and w.access_end))")
    List<Whitelist> findValidWhiteListByCar(Cars car, Date date);
}
