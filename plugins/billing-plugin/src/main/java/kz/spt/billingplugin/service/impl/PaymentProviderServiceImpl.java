package kz.spt.billingplugin.service.impl;


import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.billingplugin.repository.PaymentProviderRepository;
import kz.spt.billingplugin.service.PaymentProviderService;
import kz.spt.lib.model.dto.SelectOption;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@Log
@Service
@Transactional
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

    @Override
    public List<SelectOption> getSelectOption() {
        return ((List<PaymentProvider>)listAllPaymentProviders()).stream()
                .map(paymentProvider -> new SelectOption(paymentProvider.getId().toString(), paymentProvider.getName()))
                .collect(Collectors.toList());
    }
}
