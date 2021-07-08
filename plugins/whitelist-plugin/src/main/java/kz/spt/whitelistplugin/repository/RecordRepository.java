package kz.spt.whitelistplugin.repository;

import kz.spt.whitelistplugin.model.Whitelist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRepository extends JpaRepository<Whitelist, Long> {
}
