package kz.spt.app.service.impl;

import kz.spt.lib.service.CarImageService;
import kz.spt.lib.service.EventLogService;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Log
@Service
public class CarImageServiceImpl implements CarImageService {

    private String imagePath;
    private EventLogService eventLogService;

    public CarImageServiceImpl(@Value("${images.file.path}") String imagePath, EventLogService eventLogService) {
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
        if (!theDir.exists()) {
            theDir.mkdirs();
        }

        List<String> base64imageTypes = new ArrayList<>();
        base64imageTypes.add("data:image/jpeg;base64,");
        base64imageTypes.add("data:image/jpg;base64,");

        for (String imageType : base64imageTypes) {
            if (base64.startsWith(imageType)) {
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

        return fullPath.replace(imagePath, "");
    }

    @Override
    public void saveSnapshot(byte[] image, String ip) throws IOException {
        String fileName = ip.replace(".", "-");
        String path = imagePath;
        File theDir = new File(path);
        if (!theDir.exists()) {
            theDir.mkdirs();
        }

        String fullPath = path + "/" + fileName + StaticValues.carImageExtension;
        Files.write(Path.of(fullPath), image);
    }

    @Override
    public byte[] getByUrl(String url) throws IOException {
        File thePath = new File(imagePath + url);
        if (thePath.exists()) {
            return Files.readAllBytes(thePath.toPath());
        }
        return null;
    }
}
