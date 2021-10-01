package kz.spt.billingplugin.service;

import kz.spt.billingplugin.model.PaymentProvider;

import java.security.NoSuchAlgorithmException;

public interface PaymentProviderService {

    Iterable<PaymentProvider> listAllPaymentProviders();

    PaymentProvider getProviderById(Long id);

    void saveProvider(PaymentProvider provider) throws NoSuchAlgorithmException;

    String getClientPasswordHash(String clientId);
}
