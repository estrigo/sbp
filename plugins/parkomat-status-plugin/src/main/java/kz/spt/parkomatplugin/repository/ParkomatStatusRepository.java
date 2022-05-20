package kz.spt.parkomatplugin.repository;

import kz.spt.parkomatplugin.model.ParkomatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkomatStatusRepository extends JpaRepository<ParkomatStatus, Long> {
}
