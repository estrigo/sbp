package kz.spt.app.service.impl;

import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.CameraStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.dto.CarPictureFromRestDto;
import kz.spt.lib.service.CarImageService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
@Log
@Service
@Transactional(noRollbackFor = Exception.class)
public class CarImageServiceImpl implements CarImageService {

    private String debtPlateNumber;
    private String imagePath;
    private EventLogService eventLogService;
    private StatusCheckJob statusCheckJob;
    private CameraServiceImpl cameraServiceImpl;


    public CarImageServiceImpl(@Value("${images.file.path}") String imagePath,
                               EventLogService eventLogService, StatusCheckJob statusCheckJob,
                               CameraServiceImpl cameraServiceImpl) {
        this.imagePath = imagePath;
        this.eventLogService = eventLogService;
        this.statusCheckJob = statusCheckJob;
        this.cameraServiceImpl = cameraServiceImpl;
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
        System.out.println(url + " ------url");
        System.out.println(imagePath + url + " ------imagePath + url");
        File thePath = new File(imagePath + url);
        if (thePath.exists()) {
            return Files.readAllBytes(thePath.toPath());
        }
        return null;
    }

    @Override
    public void checkSnapshotEnabled(CarPictureFromRestDto carPictureFromRestDto) throws IOException {
        for (GateStatusDto gateStatusDto : StatusCheckJob.globalGateDtos) {
            CameraStatusDto cameraStatusDto = gateStatusDto.frontCamera;
            if (cameraStatusDto != null && cameraStatusDto.getIp().equals(carPictureFromRestDto.getIp_address())) {
                if (cameraStatusDto.snapshotEnabled != null) {
                    Map<Long, Boolean> snapshotEnabledRefreshMap = cameraServiceImpl.getSnapshotEnabledRefreshMap();
                    Boolean isSnapshotEnabled = snapshotEnabledRefreshMap.get(cameraStatusDto.id);
                    if (isSnapshotEnabled != null && !isSnapshotEnabled) {
                        return;
                    }
                }
                saveImageFromBase64ToPicture(carPictureFromRestDto);
            }
        }
    }

    private void saveImageFromBase64ToPicture(CarPictureFromRestDto carPictureFromRestDto) throws IOException {
        try {
            byte[] decodedImageBytes = Base64.decodeBase64(carPictureFromRestDto.getCar_picture());
            String ss = carPictureFromRestDto.getIp_address().replace(".", "-") + ".jpeg";
            log.info("saveImageFromBase64ToPicture-------- " + imagePath +  "/" + ss);
            log.info("saveImageFromBase64ToPicture---decodedImageBytes----- " +decodedImageBytes);
            Files.write(Paths.get(imagePath + "/" + carPictureFromRestDto.getIp_address().replace(".", "-") + ".jpeg"), decodedImageBytes);

        }
        catch (Exception e) {
            log.warning("Error of saveImageFromBase64ToPicture: " + e);
        }
    }
}
