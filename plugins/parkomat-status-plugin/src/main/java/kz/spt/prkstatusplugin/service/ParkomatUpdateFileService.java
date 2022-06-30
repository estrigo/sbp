package kz.spt.prkstatusplugin.service;

import kz.spt.prkstatusplugin.model.ParkomatUpdate;

import java.io.File;

public interface ParkomatUpdateFileService {

    boolean store(ParkomatUpdate parkomatUpdate, byte[] fileBytes);

    File getFile(ParkomatUpdate parkomatUpdate);

}
