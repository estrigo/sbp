package kz.spt.parkomatplugin.repository;

import kz.spt.parkomatplugin.model.ParkomatLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkomatLogRepository extends JpaRepository<ParkomatLog, Long> {
}
