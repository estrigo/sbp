package kz.spt.prkstatusplugin.repository;

import kz.spt.prkstatusplugin.model.ParkomatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkomatStatusRepository extends JpaRepository<ParkomatStatus, Long> {
}
