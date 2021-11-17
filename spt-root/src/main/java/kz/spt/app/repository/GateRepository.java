package kz.spt.app.repository;

import kz.spt.lib.model.Gate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GateRepository extends JpaRepository<Gate, Long> {

    Iterable<Gate> findByGateType(Gate.GateType type);
}
