package kz.spt.prkstatusplugin.service;

import kz.spt.prkstatusplugin.model.ParkomatConfig;
import kz.spt.prkstatusplugin.model.ParkomatUpdate;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ParkomatService {

    public List<?> getParkomatProviders();
    public ParkomatConfig getParkomatConfig(String ip);
    void saveParkomatConfig(ParkomatConfig config);

    void saveParkomatUpdate(ParkomatUpdate update);

    public Page<ParkomatUpdate> getUpdates();
}
