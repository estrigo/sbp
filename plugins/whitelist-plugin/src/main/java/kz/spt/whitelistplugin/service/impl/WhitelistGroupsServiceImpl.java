package kz.spt.whitelistplugin.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import kz.spt.lib.model.Parking;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.repository.WhitelistGroupsRepository;
import kz.spt.whitelistplugin.service.RootServicesGetterService;
import kz.spt.whitelistplugin.service.WhitelistGroupsService;
import kz.spt.whitelistplugin.service.WhitelistService;
import org.pf4j.util.StringUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WhitelistGroupsServiceImpl implements WhitelistGroupsService {

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";
    private WhitelistGroupsRepository whitelistGroupsRepository;
    private RootServicesGetterService rootServicesGetterService;
    private WhitelistService whitelistService;

    public WhitelistGroupsServiceImpl(WhitelistGroupsRepository whitelistGroupsRepository, RootServicesGetterService rootServicesGetterService,
                                      WhitelistService whitelistService) {
        this.whitelistGroupsRepository = whitelistGroupsRepository;
        this.rootServicesGetterService = rootServicesGetterService;
        this.whitelistService = whitelistService;
    }

    @Override
    public WhitelistGroups findById(Long id) {
        return whitelistGroupsRepository.getOne(id);
    }

    @Override
    public WhitelistGroups getWithCars(Long id) {
        return whitelistGroupsRepository.getWhitelistGroup(id);
    }

    @Override
    public WhitelistGroups saveWhitelistGroup(WhitelistGroups whitelistGroups, String currentUser) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(dateformat);
        if (Whitelist.Type.PERIOD.equals(whitelistGroups.getType())) {
            if (StringUtils.isNotNullOrEmpty(whitelistGroups.getAccessStartString())) {
                whitelistGroups.setAccess_start(format.parse(whitelistGroups.getAccessStartString()));
            }
            if (StringUtils.isNotNullOrEmpty(whitelistGroups.getAccessEndString())) {
                whitelistGroups.setAccess_end(format.parse(whitelistGroups.getAccessEndString()));
            }
        } else {
            if(!Whitelist.Type.CUSTOM.equals(whitelistGroups.getType())){
                whitelistGroups.setCustomJson(null);
            }
            whitelistGroups.setAccess_start(null);
            whitelistGroups.setAccess_end(null);
        }
        Parking parking = rootServicesGetterService.getParkingService().findById(whitelistGroups.getParkingId());
        whitelistGroups.setParking(parking);
        whitelistGroups.setUpdatedUser(currentUser);
        WhitelistGroups updatedWhitelistGroups = whitelistGroupsRepository.save(whitelistGroups);

        Set<String> updatedPlateNumbers = whitelistGroups.getPlateNumbers().stream().collect(Collectors.toSet());
        if(whitelistGroups.getId() == null){
            for (String updatedPlateNumber : updatedPlateNumbers) {
                Whitelist whitelist = whitelistService.findByPlatenumber(updatedPlateNumber, whitelistGroups.getParkingId());
                if(whitelist != null && whitelistGroups.getForceUpdate()){
                    whitelistService.deleteById(whitelist.getId());
                }
            }
        } else {
            List<Whitelist> groupWhitelists = whitelistService.listByGroupId(whitelistGroups.getId());
            List<String> groupWhitelistPlateNumbers = new ArrayList<>();
            for (Whitelist w : groupWhitelists) {
                groupWhitelistPlateNumbers.add(w.getCar().getPlatenumber());
            }

            for (String updatedPlateNumber : updatedPlateNumbers) {
                if(!groupWhitelistPlateNumbers.contains(updatedPlateNumber)){
                    Whitelist whitelist = whitelistService.findByPlatenumber(updatedPlateNumber, whitelistGroups.getParkingId());
                    if(whitelist != null && whitelistGroups.getForceUpdate()){
                        whitelistService.deleteById(whitelist.getId());
                    }
                }
            }

            for (Whitelist w : groupWhitelists) {
                if (!updatedPlateNumbers.contains(w.getCar().getPlatenumber())) {
                    whitelistService.deleteById(w.getId());
                }
            }
        }

        for (String updatedPlateNumber : updatedPlateNumbers) {
            whitelistService.saveWhitelistFromGroup(updatedPlateNumber, updatedWhitelistGroups, currentUser, parking);
        }
        return updatedWhitelistGroups;
    }

    @Override
    public void deleteGroup(WhitelistGroups group) {
        whitelistGroupsRepository.delete(group);
    }

    @Override
    public Iterable<WhitelistGroups> listAllWhitelistGroups() throws JsonProcessingException {
        List<WhitelistGroups> whitelistGroupsList = whitelistGroupsRepository.findAll();
        for(WhitelistGroups wg: whitelistGroupsList){
            wg.setConditionDetail(WhitelistServiceImpl.formConditionDetails(wg, wg.getName()));
        }
        return whitelistGroupsList;
    }

    @Override
    public void deleteById(Long id) {
        WhitelistGroups whitelistGroups = whitelistGroupsRepository.getWhitelistGroup(id);
        List<Whitelist> groupWhitelists = whitelistService.listByGroupId(whitelistGroups.getId());
        if (groupWhitelists != null) {
            for (Whitelist w : groupWhitelists) {
                whitelistService.deleteById(w.getId());
            }
        }
        whitelistGroupsRepository.deleteById(id);
    }

    @Override
    public WhitelistGroups prepareById(Long id) {
        SimpleDateFormat format = new SimpleDateFormat(dateformat);
        WhitelistGroups whitelistGroups = whitelistGroupsRepository.getWhitelistGroup(id);
        List<String> plateNumbers = new ArrayList<>();
        List<Whitelist> groupWhitelists = whitelistService.listByGroupId(whitelistGroups.getId());
        if (groupWhitelists != null) {
            for (Whitelist w : groupWhitelists) {
                plateNumbers.add(w.getCar().getPlatenumber());
            }
        }
        whitelistGroups.setPlateNumbers(plateNumbers);
        if (Whitelist.Type.PERIOD.equals(whitelistGroups.getType())) {
            if (whitelistGroups.getAccess_start() != null) {
                whitelistGroups.setAccessStartString(format.format(whitelistGroups.getAccess_start()));
            }
            whitelistGroups.setAccessEndString(format.format(whitelistGroups.getAccess_end()));
        }
        if(whitelistGroups.getParking() != null){
            whitelistGroups.setParkingId(whitelistGroups.getParking().getId());
        }

        return whitelistGroups;
    }
}
