package kz.spt.app.repository;

import kz.spt.lib.model.PosTerminal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PosTerminalRepository extends JpaRepository<PosTerminal, Long> {

    List<PosTerminal> findPosTerminalsByReconsilatedIsFalse();
}
