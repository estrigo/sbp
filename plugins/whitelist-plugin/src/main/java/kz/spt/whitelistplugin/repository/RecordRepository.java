package kz.spt.whitelistplugin.repository;

import kz.spt.whitelistplugin.model.Record;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRepository extends JpaRepository<Record, Long> {
}
