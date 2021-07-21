package kz.spt.whitelistplugin.service.impl;

import kz.spt.api.model.Cars;
import kz.spt.api.service.CarsService;
import kz.spt.whitelistplugin.WhitelistPlugin;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.repository.WhitelistRepository;
import kz.spt.whitelistplugin.service.WhitelistService;
import org.pf4j.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class WhitelistServiceImpl implements WhitelistService {

    private CarsService carsService;

    @Autowired
    private WhitelistRepository whitelistRepository;

    @Override
    public void saveWhitelist(Whitelist whitelist, UserDetails currentUser) throws Exception {
        getCarsService().createCar(whitelist.getPlatenumber());
        Cars car = getCarsService().findByPlatenumber(whitelist.getPlatenumber());
        whitelist.setCar(car);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

        if(Whitelist.Type.PERIOD.equals(whitelist.getType()) || Whitelist.Type.ONCE.equals(whitelist.getType()) || Whitelist.Type.MONTHLY.equals(whitelist.getType())){
            if(StringUtils.isNotNullOrEmpty(whitelist.getAccessStartString())){
                whitelist.setAccess_start(format.parse(whitelist.getAccessStartString()));
            }
            if(StringUtils.isNotNullOrEmpty(whitelist.getAccessEndString())){
                whitelist.setAccess_end(format.parse(whitelist.getAccessEndString()));
            }
        }
        if(whitelist.getId()!=null){
            whitelist.setUpdatedUser(currentUser.getUsername());
        } else {
            whitelist.setCreatedUser(currentUser.getUsername());
        }

        whitelistRepository.save(whitelist);
    }

    @Override
    public Iterable<Whitelist> listAllWhitelist() {
        return whitelistRepository.findAllByCarIsNotNull();
    }

    @Override
    public Boolean hasAccess(String platenumber, Date date) {

        Cars car = getCarsService().findByPlatenumber(platenumber);
        if(car!=null){
            List<Whitelist> whitelists = whitelistRepository.findValidWhiteListByCar(car, date);
            if (whitelists.size() > 0){
                return true;
            }
        }

        return false;
    }

    private CarsService getCarsService(){
        if(this.carsService == null){
            carsService = (CarsService) WhitelistPlugin.INSTANCE.getMainApplicationContext().getBean("carsServiceImpl");
        }
        return carsService;
    }
}
