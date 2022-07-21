package kz.spt.lib.service;

import kz.spt.lib.model.PaymentCheckLog;

import java.util.List;

public interface PaymentCheckLogService {

    void save(PaymentCheckLog paymentCheckLog);

    PaymentCheckLog findLastSuccessCheck(String plateNumber);


    List<PaymentCheckLog> finPaymentCheckLogByProviderId(Long providerId);
}
