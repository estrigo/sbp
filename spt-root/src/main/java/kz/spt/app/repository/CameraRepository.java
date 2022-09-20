package kz.spt.app.repository;

import kz.spt.lib.model.Camera;
import kz.spt.lib.model.Gate;
import kz.spt.lib.model.Parking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CameraRepository extends JpaRepository<Camera, Long> {

    @Query("from Camera c LEFT JOIN FETCH c.gate g LEFT JOIN FETCH g.parking WHERE c.ip = ?1")
    List<Camera> findCameraByIp(String ip);

    @Query("from Camera c LEFT JOIN FETCH c.gate g LEFT JOIN FETCH g.parking WHERE c.id = ?1")
    Camera findCameraById(Long id);

    @Query("from Camera c WHERE c.enabled = true")
    List<Camera> findEnabledCameras();

    @Query("from Camera c WHERE c.enabled = true and c.cameraTab.id = ?1")
    List<Camera> findEnabledWithTabCameras(Long id);

    @Query("from Camera c WHERE c.enabled = true and c.cameraTab is null")
    List<Camera> findEnabledWithoutTabCameras();

    @Query("from Camera c LEFT JOIN FETCH c.gate g LEFT JOIN FETCH g.parking WHERE c.detectorId = ?1")
    Camera findCameraByDetectorId(String detectorId);

    Optional<Camera> findCameraByIpAndGate_Parking(String ip, Parking parking);

}
