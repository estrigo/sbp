package kz.spt.prkstatusplugin.service;

import kz.spt.prkstatusplugin.model.ParkomatUpdate;

public interface ParkomatUpdateFileService {

    boolean store(ParkomatUpdate parkomatUpdate, byte[] fileBytes);
}
