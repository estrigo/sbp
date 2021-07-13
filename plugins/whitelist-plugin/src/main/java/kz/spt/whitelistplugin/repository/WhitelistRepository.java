package kz.spt.whitelistplugin.repository;

import kz.spt.whitelistplugin.model.Whitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhitelistRepository extends JpaRepository<Whitelist, Long> {

    Iterable<Whitelist> findAllByCarIsNotNull();
}
