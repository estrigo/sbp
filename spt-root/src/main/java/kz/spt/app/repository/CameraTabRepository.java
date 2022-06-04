package kz.spt.app.repository;

import kz.spt.lib.model.CameraTab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CameraTabRepository extends JpaRepository<CameraTab, Long> {

}
