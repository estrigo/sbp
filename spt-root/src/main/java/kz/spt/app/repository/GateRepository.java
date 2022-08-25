package kz.spt.app.repository;

import kz.spt.lib.model.Gate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GateRepository extends JpaRepository<Gate, Long> {

    Iterable<Gate> findByGateType(Gate.GateType type);

    @Query("from Gate g LEFT JOIN FETCH g.cameraList cl LEFT JOIN FETCH g.barrier b")
    Iterable<Gate> findByGatesByDependents();

    Gate findFirstByGateTypeAndQrPanelIpNotNull(Gate.GateType type);

    Gate findFirstByTabloIpAndGateType(String tabloIp, Gate.GateType type);
}
