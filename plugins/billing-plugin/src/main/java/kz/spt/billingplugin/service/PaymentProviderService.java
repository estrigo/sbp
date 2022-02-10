package kz.spt.billingplugin.service;

import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.lib.model.dto.SelectOption;

import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface PaymentProviderService {

    Iterable<PaymentProvider> listAllPaymentProviders();

    PaymentProvider getProviderById(Long id);

    void saveProvider(PaymentProvider provider) throws NoSuchAlgorithmException;

    PaymentProvider getProviderByClientId(String clientId);

    List<SelectOption> getSelectOption();
}
