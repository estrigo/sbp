package kz.spt.app.service.impl;

import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.CameraStatusDto;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
@Log
@Service
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
        File thePath = new File(imagePath + url);
        if (thePath.exists()) {
            return Files.readAllBytes(thePath.toPath());
        }
        return null;
    }

    @Override
    public void checkSnapshotEnabled(CarPictureFromRestDto carPictureFromRestDto) {
        if (carPictureFromRestDto != null) {
            CameraStatusDto cameraStatusDtoByIp = statusCheckJob.findCameraStatusDtoByIp(carPictureFromRestDto.getIp_address());

            if (cameraStatusDtoByIp != null) {
                Map<Long, Boolean> snapshotEnabledRefreshMap = cameraServiceImpl.getSnapshotEnabledRefreshMap();
                Boolean snapshotEnabled = snapshotEnabledRefreshMap.get(cameraStatusDtoByIp.id);
                if (!(snapshotEnabled != null && !snapshotEnabled)) {
                    sendSocketArmPicture(cameraStatusDtoByIp, carPictureFromRestDto);
                }
            }
        }
    }

    private void sendSocketArmPicture(CameraStatusDto cameraStatusDtoByIp, CarPictureFromRestDto carPictureFromRestDto) {
        eventLogService.sendSocketMessage(EventLogService.ArmEventType.Picture, EventLog.StatusType.Success,
                cameraStatusDtoByIp.id, debtPlateNumber, carPictureFromRestDto.getCar_picture(), null);
    }
}
