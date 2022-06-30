package kz.spt.prkstatusplugin.repository;

import kz.spt.prkstatusplugin.model.ParkomatLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkomatLogRepository extends JpaRepository<ParkomatLog, Long> {
}
