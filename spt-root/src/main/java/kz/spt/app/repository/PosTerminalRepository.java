package kz.spt.app.repository;

import kz.spt.lib.model.PosTerminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PosTerminalRepository extends JpaRepository<PosTerminal, Long> {

    List<PosTerminal> findPosTerminalsByReconsilatedIsFalse();

    @Query("from PosTerminal p where p.type = ?1 and p.reconsilated = false")
    List<PosTerminal> findPosTerminalsByReconsilatedIsFalseAndType(PosTerminal.terminalType type);

}
