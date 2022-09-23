package kz.spt.reportplugin.service.impl;

import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.service.PluginService;
import kz.spt.reportplugin.ReportPlugin;
import kz.spt.reportplugin.service.RootServicesGetterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(noRollbackFor = Exception.class)
public class RootServicesGetterServiceImpl implements RootServicesGetterService {
    private PluginService pluginService;
    private CarStateService carStateService;
    private EventLogService eventLogService;

    @Override
    public PluginService getPluginService() {
        if (this.pluginService == null) {
            pluginService = (PluginService) ReportPlugin.INSTANCE.getMainApplicationContext().getBean("pluginServiceImpl");
        }

        return this.pluginService;
    }

    @Override
    public CarStateService getCarStateService() {
        if (this.carStateService == null) {
            carStateService = (CarStateService) ReportPlugin.INSTANCE.getMainApplicationContext().getBean("carStateServiceImpl");
        }

        return this.carStateService;
    }

    @Override
    public EventLogService getEventLogService() {
        if (this.eventLogService == null) {
            eventLogService = (EventLogService) ReportPlugin.INSTANCE.getMainApplicationContext().getBean("eventLogServiceImpl");
        }

        return this.eventLogService;
    }
}
