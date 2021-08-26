package kz.spt.billingplugin.service;

import kz.spt.billingplugin.model.Payment;

public interface PaymentService {

    Iterable<Payment> listAllPayments();
}
