package kz.spt.billingplugin.service;

import kz.spt.billingplugin.model.Payment;

import java.util.Date;
import java.util.List;

public interface PaymentService {

    Iterable<Payment> listAllPayments();

    Payment savePayment(Payment payment);

    List<Payment> getPaymentsByCarStateId(Long carStateId);

    void updateOutTimestamp(Long carStateId, Date outTimestamp);
}
