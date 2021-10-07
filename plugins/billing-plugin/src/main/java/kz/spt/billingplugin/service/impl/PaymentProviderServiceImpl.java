package kz.spt.billingplugin.service.impl;


import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.billingplugin.repository.PaymentProviderRepository;
import kz.spt.billingplugin.service.PaymentProviderService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;

@Log
@Service
public class PaymentProviderServiceImpl implements PaymentProviderService {

    @Autowired
    PaymentProviderRepository paymentProviderRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public Iterable<PaymentProvider> listAllPaymentProviders() {
        return paymentProviderRepository.findAll();
    }

    @Override
    public PaymentProvider getProviderById(Long id) {
        return paymentProviderRepository.getPaymentProvider(id);
    }

    @Override
    public void saveProvider(PaymentProvider provider) throws NoSuchAlgorithmException {
        if(provider.getPassword() != null && !"".equals(provider.getPassword())){
            provider.setSecret(bCryptPasswordEncoder.encode(provider.getPassword()));
        }
        paymentProviderRepository.save(provider);
    }

    @Override
    public PaymentProvider getProviderByClientId(String clientId) {
        return paymentProviderRepository.findByClientId(clientId);
    }
}
