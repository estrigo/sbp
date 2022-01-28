package kz.spt.app.repository;

import kz.spt.lib.model.Calibration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CalibrationRepository extends JpaRepository<Calibration,Long> {
    @Query("select t from Calibration t" +
            " where t.cameraId=:cameraId")
    Optional<Calibration> findByCamera(@Param("cameraId") Long cameraId);

    @Query("select t from Calibration t" +
            " where t.ip=:ip")
    Optional<Calibration> findByIp(@Param("ip") String ip);
}
