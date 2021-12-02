package kz.spt.app.service.impl;

import kz.spt.lib.model.EventLog;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.service.CarImageService;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

@Log
@Service
public class CarImageServiceImpl implements CarImageService {

    private String imagePath;
    private EventLogService eventLogService;

    public CarImageServiceImpl(@Value("${images.file.path}") String imagePath, EventLogService eventLogService){
        this.imagePath = imagePath;
        this.eventLogService = eventLogService;
    }

    @Override
    public String saveImage(String base64, Date eventDate, String carNumber) throws IOException {

        SimpleDateFormat format = new SimpleDateFormat("HH-mm-ss");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(eventDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DATE);

        String fileName = carNumber + "_" + format.format(calendar.getTime());
        String path = imagePath + "/" + year + "/" + month + "/" + day + "/";
        File theDir = new File(path);
        if (!theDir.exists()){
            theDir.mkdirs();
        }

        List<String> base64imageTypes = new ArrayList<>();
        base64imageTypes.add("data:image/jpeg;base64,");
        base64imageTypes.add("data:image/jpg;base64,");

        for(String imageType : base64imageTypes){
            if(base64.startsWith(imageType)){
                base64 = base64.replaceFirst(imageType, "");
            }
        }
        String base64Raw = base64;
        byte[] imageBytes = Base64.decodeBase64(base64Raw);

        String fullPath = path + fileName + StaticValues.carImageExtension;
        Files.write(Path.of(fullPath), imageBytes);

        String resizedFileName = fileName + StaticValues.carImageSmallAddon;
        String resizedfullPath = path + resizedFileName + StaticValues.carImageExtension;

        Thumbnails.of(fullPath)
                .size(200, 100)
                .outputFormat("JPEG")
                .outputQuality(1)
                .toFile(resizedfullPath);

        return fullPath.replace(imagePath,"");
    }

    @Override
    public byte[] getImage(Long eventId) throws IOException {

        EventLog eventLog = eventLogService.getById(eventId);

        if(eventLog!=null && eventLog.getProperties()!=null && eventLog.getProperties().containsKey(StaticValues.carImagePropertyName)) {
            File thePath = new File(imagePath + eventLog.getProperties().get(StaticValues.carImagePropertyName));
            if (thePath.exists()) {
                return Files.readAllBytes(thePath.toPath());
            }
        }
        return null;
    }

    @Override
    public byte[] getSmallImage(Long eventId) throws IOException {

        EventLog eventLog = eventLogService.getById(eventId);

        if(eventLog!=null && eventLog.getProperties()!=null && eventLog.getProperties().containsKey(StaticValues.carSmallImagePropertyName)) {
            File thePath = new File(imagePath + eventLog.getProperties().get(StaticValues.carSmallImagePropertyName));
            if (thePath.exists()) {
                return Files.readAllBytes(thePath.toPath());
            }
        }
        return null;
    }

    @Override
    public void fixSmall() throws IOException {
        List<EventLog> eventLogs = (List<EventLog>) eventLogService.listAllLogs();
        for(EventLog eventLog:eventLogs){
            Map<String, Object> props = eventLog.getProperties();
            if(props != null && props.containsKey(StaticValues.carImagePropertyName)) {
                String carImageUrl = (String) props.get(StaticValues.carImagePropertyName);
                if(carImageUrl.contains(imagePath)){
                    carImageUrl = carImageUrl.replace(imagePath, "");
                    props.put(StaticValues.carImagePropertyName, carImageUrl);
                }
                if(!props.containsKey(StaticValues.carSmallImagePropertyName)){
                    String fullPath = imagePath + carImageUrl;
                    String resizedfullPath = imagePath + carImageUrl.replace(StaticValues.carImageExtension,"") + StaticValues.carImageSmallAddon + StaticValues.carImageExtension;
                    Thumbnails.of(fullPath)
                            .size(200, 100)
                            .outputFormat("JPEG")
                            .outputQuality(1)
                            .toFile(resizedfullPath);
                    props.put(StaticValues.carSmallImagePropertyName, resizedfullPath.replace(imagePath, ""));
                    eventLog.setProperties(props);
                    eventLogService.save(eventLog);
                    log.info("saving event log: "  + eventLog.getId() + " " + StaticValues.carImagePropertyName
                            + ": " + props.get(StaticValues.carImagePropertyName) + " " + StaticValues.carSmallImagePropertyName + ": " + props.get(StaticValues.carSmallImagePropertyName));
                }
            }
        }
    }
}
