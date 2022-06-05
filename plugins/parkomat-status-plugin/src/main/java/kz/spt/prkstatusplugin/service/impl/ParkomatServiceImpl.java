package kz.spt.prkstatusplugin.service.impl;


import kz.spt.prkstatusplugin.service.ParkomatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkomatServiceImpl implements ParkomatService {

    @Override
    public List<?> getParkomatProviders() {

       // paymentProviderRepository.getPaymentProvider(0L);

        return null;
    }
}
