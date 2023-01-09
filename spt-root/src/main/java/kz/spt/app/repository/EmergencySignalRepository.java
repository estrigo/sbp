package kz.spt.app.repository;

import kz.spt.lib.model.EmergencySignalConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergencySignalRepository extends JpaRepository<EmergencySignalConfig, String> {
}
