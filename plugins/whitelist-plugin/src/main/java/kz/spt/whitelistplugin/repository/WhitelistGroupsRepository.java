package kz.spt.whitelistplugin.repository;


import kz.spt.whitelistplugin.model.WhitelistGroups;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WhitelistGroupsRepository extends JpaRepository<WhitelistGroups, Long> {

    @Query("from WhitelistGroups w where w.id = ?1")
    WhitelistGroups getWhitelistGroup(Long id);
}
