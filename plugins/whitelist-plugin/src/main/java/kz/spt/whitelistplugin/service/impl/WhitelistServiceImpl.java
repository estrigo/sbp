package kz.spt.whitelistplugin.service.impl;

import kz.spt.api.model.Cars;
import kz.spt.api.service.CarsService;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.repository.WhitelistRepository;
import kz.spt.whitelistplugin.service.WhitelistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class WhitelistServiceImpl implements WhitelistService {

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
        if(whitelist.getAccessStartString() != null){
            whitelist.setAccess_start(format.parse(whitelist.getAccessStartString()));
        }
        if(whitelist.getAccessEndString() != null){
            whitelist.setAccess_end(format.parse(whitelist.getAccessEndString()));
        }
        whitelistRepository.save(whitelist);
    }

    @Override
    public Iterable<Whitelist> listAllWhitelist() {
        return whitelistRepository.findAllByCarIsNotNull();
    }

    @Override
    public Boolean hasAccess(String platenumber, Date date) {

        Cars car = carsService.findByPlatenumber(platenumber);
        if(car!=null){
            List<Whitelist> whitelists = whitelistRepository.findValidWhiteListByCar(car, date);
            if (whitelists.size() > 0){
                return true;
            }
        }

        return false;
    }
}
