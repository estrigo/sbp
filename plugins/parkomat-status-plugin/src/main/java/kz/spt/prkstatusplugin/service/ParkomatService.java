package kz.spt.prkstatusplugin.service;

import kz.spt.prkstatusplugin.enums.SoftwareType;
import kz.spt.prkstatusplugin.model.ParkomatConfig;
import kz.spt.prkstatusplugin.model.ParkomatUpdate;
import kz.spt.prkstatusplugin.model.PaymentProvider;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ParkomatService {

    public List<?> getParkomatProviders();
    public ParkomatConfig getParkomatConfig(String ip);

    public PaymentProvider getParkomatByIP(String ip);
    void saveParkomatConfig(ParkomatConfig config);

    void saveParkomatUpdate(ParkomatUpdate update);

    public Page<ParkomatUpdate> getUpdates(SoftwareType type, int limit);

    public ParkomatUpdate getUpdate(long id);
}
