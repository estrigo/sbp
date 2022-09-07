package kz.spt.prkstatusplugin.repository;

import kz.spt.prkstatusplugin.model.PosTerminal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PosTerminalRepository extends JpaRepository<PosTerminal, Long> {

    List<PosTerminal> findPosTerminalsByReconsilatedIsTrue();
}
