package kz.spt.parkomatplugin.service.impl;

import kz.spt.billingplugin.repository.PaymentProviderRepository;
import kz.spt.parkomatplugin.service.ParkomatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkomatServiceImpl implements ParkomatService {

    @Autowired
    private PaymentProviderRepository paymentProviderRepository;
    @Override
    public List<?> getParkomatProviders() {

        paymentProviderRepository.getPaymentProvider(0L);

        return null;
    }
}
