package kz.spt.app.service.impl;

import kz.spt.app.repository.PaymentCheckLogRepository;
import kz.spt.lib.service.PaymentCheckLogService;
import kz.spt.lib.model.PaymentCheckLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PaymentCheckLogServiceImpl implements PaymentCheckLogService {

    private final PaymentCheckLogRepository paymentCheckLogRepository;

    public PaymentCheckLogServiceImpl(PaymentCheckLogRepository paymentCheckLogRepository){
        this.paymentCheckLogRepository = paymentCheckLogRepository;
    }

    @Override
    public void save(PaymentCheckLog paymentCheckLog) {
        paymentCheckLogRepository.save(paymentCheckLog);
    }

    @Override
    public PaymentCheckLog findLastSuccessCheck(String plateNumber) {
        Pageable first = PageRequest.of(0, 1);
        Page<PaymentCheckLog> page = paymentCheckLogRepository.findLastSuccessCheck(plateNumber, first);
        if(page != null && page.toList().size() > 0){
            return page.toList().get(0);
        }
        return null;
    }
}
