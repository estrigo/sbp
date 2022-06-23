package kz.spt.prkstatusplugin.service.impl;


import kz.spt.prkstatusplugin.controller.ParkomatStatusController;
import kz.spt.prkstatusplugin.model.ParkomatConfig;
import kz.spt.prkstatusplugin.model.ParkomatUpdate;
import kz.spt.prkstatusplugin.model.PaymentProvider;
import kz.spt.prkstatusplugin.repository.ParkomatConfigRepository;
import kz.spt.prkstatusplugin.repository.ParkomatUpdateRepository;
import kz.spt.prkstatusplugin.repository.PaymentProviderRepository;
import kz.spt.prkstatusplugin.service.ParkomatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ParkomatServiceImpl implements ParkomatService {


    @Autowired
    PaymentProviderRepository paymentProviderRepository;

    @Autowired
    ParkomatConfigRepository parkomatConfigRepository;

    @Autowired
    ParkomatUpdateRepository parkomatUpdateRepository;


    @Override
    public List<PaymentProvider> getParkomatProviders() {
        return paymentProviderRepository.getParkomatProviders();
    }

    @Override
    public ParkomatConfig getParkomatConfig(String ip) {
        Optional<ParkomatConfig> parkomatConfig = parkomatConfigRepository.findById(ip);
        if (parkomatConfig.isPresent())
            return parkomatConfig.get();
        return null;

    }

    @Override
    public void saveParkomatConfig(ParkomatConfig config) {
        parkomatConfigRepository.save(config);
    }

    @Override
    public void saveParkomatUpdate(ParkomatUpdate update) {
        parkomatUpdateRepository.save(update);
    }

    @Override
    public Page<ParkomatUpdate> getUpdates() {
        return parkomatUpdateRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")));
    }


}
