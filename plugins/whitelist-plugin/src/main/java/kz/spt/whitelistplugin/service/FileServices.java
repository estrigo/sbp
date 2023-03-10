package kz.spt.whitelistplugin.service;

import kz.spt.lib.model.Cars;
import kz.spt.lib.model.Customer;
import kz.spt.lib.model.Parking;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.ParkingService;
import kz.spt.whitelistplugin.model.AbstractWhitelist;
import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.model.WhitelistGroups;
import kz.spt.whitelistplugin.repository.WhitelistGroupsRepository;
import kz.spt.whitelistplugin.repository.WhitelistRepository;
import kz.spt.whitelistplugin.utils.ExcelUtils;
import lombok.extern.flogger.Flogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

@Service
@Slf4j
@Transactional(noRollbackFor = Exception.class)
public class FileServices {

    @Autowired
    WhitelistRepository whitelistRepository;

    @Autowired
    WhitelistGroupsRepository whitelistGroupsRepository;

    @Autowired
    private WhitelistService whitelistService;
    private ParkingService parkingService;

    private CarsService carsService;
    private RootServicesGetterService rootServicesGetterService;

    // Store File Data to Database
    public void store(MultipartFile file, Parking parking, RootServicesGetterService rootServicesGetterService, UserDetails currentUser){

        parkingService = rootServicesGetterService.getParkingService();
        carsService = rootServicesGetterService.getCarsService();

        try {
            List<Pair<Whitelist, String>> whitelists = ExcelUtils.parseExcelFileWhiteList(file.getInputStream(), parking);

            for (int i = 0; i < whitelists.size(); i++) {
                Whitelist whitelist = whitelists.get(i).getFirst();
                WhitelistGroups whitelistGroups = whitelistGroupsRepository.getWhitelistGroupsByNameAndParking(whitelists.get(i).getSecond(), parking);
                if (whitelistGroups == null) {
                    WhitelistGroups newWhitelistGroups = new WhitelistGroups();
                    newWhitelistGroups.setName(whitelists.get(i).getSecond());
                    newWhitelistGroups.setParking(parking);
                    newWhitelistGroups.setType(AbstractWhitelist.Type.UNLIMITED);
                    newWhitelistGroups = whitelistGroupsRepository.saveAndFlush(newWhitelistGroups);
                    whitelist.setGroupId(newWhitelistGroups.getId());
                } else {
                    whitelist.setGroupId(whitelistGroups.getId());
                }
                try {
                    whitelistService.saveWhitelist(whitelist, currentUser);
                } catch (Exception e) {
                   log.info("the next plate number was not saved: " + whitelist.getPlatenumber());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("FAIL! -> message = " + e.getMessage());
        }
    }
}
