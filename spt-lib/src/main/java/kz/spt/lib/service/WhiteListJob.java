package kz.spt.lib.service;

import org.springframework.http.ResponseEntity;

public interface WhiteListJob {
    ResponseEntity<?> startWhiteListJob();
    ResponseEntity<?> stopWhiteListJob();



}