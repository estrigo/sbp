package kz.spt.app.service.impl;

import kz.spt.app.repository.PosTerminalRepository;
import kz.spt.lib.exception.ErrorMessage;
import kz.spt.lib.service.PosTerminalService;
import kz.spt.lib.exception.TerminalStatusException;
import kz.spt.lib.model.PosTerminal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(noRollbackFor = Exception.class)
public class PosTerminalServiceImpl implements PosTerminalService {

    private final PosTerminalRepository posTerminalRepository;

    public PosTerminalServiceImpl(PosTerminalRepository posTerminalRepository) {
        this.posTerminalRepository = posTerminalRepository;
    }

    @Override
    public ResponseEntity<ErrorMessage> checkNotClosedTerminals() throws TerminalStatusException {
        List<PosTerminal> posTerminalList =
                posTerminalRepository.findPosTerminalsByReconsilatedIsFalseAndType(PosTerminal.terminalType.TERMINAL);
        String listOfNotReconciliated = posTerminalList.stream()
                .map(PosTerminal::getIp)
                .collect(Collectors.joining(", "));
        try {
            if (!posTerminalList.isEmpty()) {
                throw new TerminalStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Not reconciliated: " + listOfNotReconciliated);
            } else return null;
        } catch (TerminalStatusException ex) {
            return new ResponseEntity<>(new ErrorMessage(
                    LocalDateTime.now(),
                    ex.getMessage()), ex.getStatus());
        }
    }

}
