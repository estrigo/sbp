package kz.spt.billingplugin.service.impl;

import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.repository.PaymentRepository;
import kz.spt.billingplugin.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentRepository paymentRepository;

    @Override
    public Iterable<Payment> listAllPayments() {
        return paymentRepository.findAll();
    }
}
