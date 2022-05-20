package kz.spt.parkomatplugin.service;

import kz.spt.billingplugin.repository.PaymentProviderRepository;

import java.util.List;

public interface ParkomatService {

    public List<?> getParkomatProviders();
}
