package kz.spt.lib.service;

import kz.spt.lib.model.PaymentCheckLog;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentCheckLogService {

    void save(PaymentCheckLog paymentCheckLog);

    PaymentCheckLog findLastSuccessCheck(String plateNumber);


    List<PaymentCheckLog> finPaymentCheckLogByProviderId(Long providerId, Pageable pageable);
}
