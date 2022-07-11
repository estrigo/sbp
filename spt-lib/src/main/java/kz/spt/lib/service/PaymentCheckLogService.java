package kz.spt.lib.service;

import kz.spt.lib.model.PaymentCheckLog;

public interface PaymentCheckLogService {

    void save(PaymentCheckLog paymentCheckLog);

    PaymentCheckLog findLastSuccessCheck(String plateNumber);
}
