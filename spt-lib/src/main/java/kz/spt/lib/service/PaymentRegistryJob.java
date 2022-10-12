package kz.spt.lib.service;

import org.springframework.http.ResponseEntity;

public interface PaymentRegistryJob {

    ResponseEntity<?> startPaymentRegistryJob() throws Exception;
    ResponseEntity<?> stopPaymentRegistryJob() throws Exception;
    String getCronValueByKey(String key);
}
