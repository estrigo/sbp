package kz.spt.parkomatplugin.repository;

import kz.spt.parkomatplugin.model.ParkomatLog;
import kz.spt.parkomatplugin.model.ParkomatUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkomatUpdateRepository extends JpaRepository<ParkomatUpdate, Long> {
}
