package kz.spt.lib.service;

import kz.spt.lib.exception.ErrorMessage;
import kz.spt.lib.exception.TerminalStatusException;
import org.springframework.http.ResponseEntity;

public interface PosTerminalService {

    ResponseEntity<ErrorMessage> checkNotClosedTerminals()  throws TerminalStatusException;
}
