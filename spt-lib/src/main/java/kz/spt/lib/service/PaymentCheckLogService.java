package kz.spt.lib.service;

import kz.spt.lib.model.PaymentCheckLog;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;

public interface PaymentCheckLogService {

    void save(PaymentCheckLog paymentCheckLog);

    PaymentCheckLog findLastSuccessCheck(String plateNumber, Date lastPaymentDate);

    List<PaymentCheckLog> finPaymentCheckLogByProviderId(Long providerId, Pageable pageable);
}
