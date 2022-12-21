package kz.spt.abonomentplugin.service;

import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.LanguagePropertiesService;
import kz.spt.lib.service.ParkingService;
import kz.spt.lib.service.PluginService;

import java.math.BigDecimal;

public interface RootServicesGetterService {

    CarsService getCarsService();

    ParkingService getParkingService();

    PluginService getPluginService();
    LanguagePropertiesService getLanguageService();

    BigDecimal getBalance(String plateNumber) throws Exception;
    void decreaseBalance(String plateNumber, BigDecimal value, String parkingName) throws Exception;
}
