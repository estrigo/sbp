package kz.spt.reportplugin.service;


import kz.spt.lib.service.*;

public interface RootServicesGetterService {
    PluginService getPluginService();
    CarStateService getCarStateService();
    EventLogService getEventLogService();
}
