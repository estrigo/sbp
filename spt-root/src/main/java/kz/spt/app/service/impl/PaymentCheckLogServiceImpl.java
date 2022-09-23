package kz.spt.app.service.impl;

import kz.spt.app.repository.PaymentCheckLogRepository;
import kz.spt.lib.service.PaymentCheckLogService;
import kz.spt.lib.model.PaymentCheckLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
@Transactional(noRollbackFor = Exception.class)
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

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, -10);
        List<PaymentCheckLog> list = paymentCheckLogRepository.findLastSuccessCheck(plateNumber, calendar.getTime());
        if(list != null && list.size() > 0){
            return list.get(0);
        }
        return null;
    }

    @Override
    public List<PaymentCheckLog> finPaymentCheckLogByProviderId(Long providerId, Pageable pageable) {
        return paymentCheckLogRepository.findPaymentCheckLogByProviderId(providerId, pageable);
    }
}
