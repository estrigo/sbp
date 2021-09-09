package kz.spt.app.repository;

import kz.spt.lib.model.Camera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CameraRepository extends JpaRepository<Camera, Long> {

    Camera findCameraByIp(String ip);
}
