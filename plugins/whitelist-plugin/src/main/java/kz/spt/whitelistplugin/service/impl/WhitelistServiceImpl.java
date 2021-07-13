package kz.spt.whitelistplugin.service.impl;

import kz.spt.api.extension.PluginRegister;
import kz.spt.api.model.Cars;
import kz.spt.api.service.CarsService;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.repository.WhitelistRepository;
import kz.spt.whitelistplugin.service.WhitelistService;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;

@Extension
@Service
public class WhitelistServiceImpl implements PluginRegister, WhitelistService {

    @Autowired
    private CarsService carsService;

    @Autowired
    private WhitelistRepository whitelistRepository;

    @Override
    public void saveWhitelist(Whitelist whitelist) throws Exception {
        carsService.createCar(whitelist.getPlatenumber());
        Cars car = carsService.findByPlatenumber(whitelist.getPlatenumber());
        whitelist.setCar(car);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        whitelist.setAccess_start(format.parse(whitelist.getAccessStartString()));
        whitelist.setAccess_end(format.parse(whitelist.getAccessEndString()));

        whitelistRepository.save(whitelist);
    }

    @Override
    public Iterable<Whitelist> listAllWhitelist() {
        return whitelistRepository.findAllByCarIsNotNull();
    }
}
