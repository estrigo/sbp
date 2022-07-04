package kz.spt.qrpanel.repository;


import kz.spt.qrpanel.model.GateOut;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GateOutRepository extends JpaRepository<GateOut, Long> {
    Iterable<GateOut> findByGateType(GateOut.GateType type);
}