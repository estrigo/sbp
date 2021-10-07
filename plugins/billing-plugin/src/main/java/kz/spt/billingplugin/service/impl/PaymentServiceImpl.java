package kz.spt.billingplugin.service.impl;

import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.repository.PaymentRepository;
import kz.spt.billingplugin.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentRepository paymentRepository;

    @Override
    public Iterable<Payment> listAllPayments() {
        return paymentRepository.listAllPaymentsWithParkings();
    }

    @Override
    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public List<Payment> getPaymentsByCarStateId(Long carStateId) {
        return paymentRepository.getPaymentsByCarStateIdWithProvider(carStateId);
    }

    @Override
    public void updateOutTimestamp(Long carStateId, Date outTimestamp) {
        List<Payment> payments = paymentRepository.getPaymentsByCarStateIdWithProvider(carStateId);
        for(Payment payment: payments){
            payment.setOutDate(outTimestamp);
        }
        paymentRepository.saveAll(payments);
    }
}
