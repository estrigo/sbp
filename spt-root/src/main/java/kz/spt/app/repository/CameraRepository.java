package kz.spt.app.repository;

import kz.spt.lib.model.Camera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CameraRepository extends JpaRepository<Camera, Long> {

    @Query("from Camera c LEFT JOIN FETCH c.gate g LEFT JOIN FETCH g.parking WHERE c.ip = ?1")
    Camera findCameraByIp(String ip);

    @Query("from Camera c LEFT JOIN FETCH c.gate g LEFT JOIN FETCH g.parking WHERE c.id = ?1")
    Camera findCameraById(Long id);

    @Query("from Camera c WHERE c.enabled = true")
    List<Camera> findEnabledCameras();
}
