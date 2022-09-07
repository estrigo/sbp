package kz.spt.prkstatusplugin.service.impl;

import kz.spt.prkstatusplugin.model.ParkomatUpdate;
import kz.spt.prkstatusplugin.model.PosTerminal;
import kz.spt.prkstatusplugin.repository.PosTerminalRepository;
import kz.spt.prkstatusplugin.service.ParkomatUpdateFileService;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
public class ParkomatUpdateFileServiceImpl implements ParkomatUpdateFileService {

    @Value("${images.file.path}")
    String imagePath;

    @Value("${terminal.reconsilation.enable:false}")
    Boolean terminalReconsilation;

    private PosTerminalRepository posTerminalRepository;

    public ParkomatUpdateFileServiceImpl (PosTerminalRepository posTerminalRepository) {
        this.posTerminalRepository = posTerminalRepository;
    }

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
        return new File( imagePath + "/updates/" + parkomatUpdate.getId() + "/update.zip");
    }

    @Scheduled(cron = "0 02 17 * * ?")
    public void terminalNightSchedule() {
        log.info("terminalNightSchedule !!!");
        List<PosTerminal> posTerminalList = posTerminalRepository.findPosTerminalsByReconsilatedIsTrue();
        log.info("posTerminalList size: " + posTerminalList.size());
        for (PosTerminal pt : posTerminalList) {
            log.info("ip: " + pt.getIp());
        }
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://10.66.57.100:8080/dump/bank/batches?key=12345678";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        log.info("response: " + response);
    }


}
