package kz.spt.prkstatusplugin.service.impl;

import kz.spt.prkstatusplugin.model.ParkomatUpdate;
import kz.spt.prkstatusplugin.service.ParkomatUpdateFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class ParkomatUpdateFileServiceImpl implements ParkomatUpdateFileService {

    @Value("${images.file.path}")
    String imagePath;

    @Override
    public boolean store(ParkomatUpdate parkomatUpdate, byte[] file) {
        try {
            String filePath = imagePath + "/updates/" + parkomatUpdate.getId() + "/update.zip";
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.write(path, file);
            return true;
        } catch (Exception ex) {
            log.error("Error while saving file ", ex);
        }
        return false;
    }

    @Override
    public File getFile(ParkomatUpdate parkomatUpdate) {
        return new File(imagePath + "/updates/" + parkomatUpdate.getId() + "/update.zip");
    }

}
