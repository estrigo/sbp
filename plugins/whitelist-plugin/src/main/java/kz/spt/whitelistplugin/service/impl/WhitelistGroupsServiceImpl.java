package kz.spt.whitelistplugin.service.impl;


import kz.spt.lib.model.Cars;
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
import java.util.Arrays;
import java.util.HashSet;
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
        return whitelistGroupsRepository.getWhitelistGroupsWithCars(id);
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
        }  else {
            whitelistGroups.setAccess_start(null);
            whitelistGroups.setAccess_end(null);
        }
        whitelistGroups.setUpdatedUser(currentUser);
        WhitelistGroups updatedWhitelistGroups = whitelistGroupsRepository.save(whitelistGroups);

        Set<String> plateNumbers = whitelistGroups.getPlateNumbers().stream().collect(Collectors.toSet());
        for(String plateNumber : plateNumbers){
            whitelistService.saveWhitelistFromGroup(plateNumber, updatedWhitelistGroups, currentUser);
        }
        return updatedWhitelistGroups;
    }

    @Override
    public void deleteGroup(WhitelistGroups group) {
        whitelistGroupsRepository.delete(group);
    }

    @Override
    public Iterable<WhitelistGroups> listAllWhitelistGroups() {
        return whitelistGroupsRepository.findAll();
    }
}
