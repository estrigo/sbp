package kz.spt.prkstatusplugin.repository;

import kz.spt.prkstatusplugin.model.ParkomatUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ParkomatUpdateRepository extends JpaRepository<ParkomatUpdate, Long>, JpaSpecificationExecutor<ParkomatUpdate> {



}
