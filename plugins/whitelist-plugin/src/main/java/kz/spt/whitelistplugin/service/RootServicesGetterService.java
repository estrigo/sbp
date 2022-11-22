package kz.spt.whitelistplugin.service;

import kz.spt.lib.service.*;

public interface RootServicesGetterService {

    CarsService getCarsService();

    CarStateService getCarStateService();

    ParkingService getParkingService();

    AdminService getAdminService();

    LanguagePropertiesService getLanguageService();
}
