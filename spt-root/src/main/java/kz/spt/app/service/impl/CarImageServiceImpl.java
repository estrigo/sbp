package kz.spt.app.service.impl;

import kz.spt.app.component.HttpRequestFactoryDigestAuth;
import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.lib.model.dto.CameraStatusDto;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.dto.CarPictureFromRestDto;
import kz.spt.lib.service.CarImageService;
import kz.spt.lib.service.EventLogService;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import lombok.val;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    @Value("${hardcode.string.noPicture}")
    private String noPicture;

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
        log.info("path: " + path);
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
    public void checkSnapshotEnabled(CarPictureFromRestDto carPictureFromRestDto) throws IOException {
        for (GateStatusDto gateStatusDto : StatusCheckJob.globalGateDtos) {
            CameraStatusDto cameraStatusDto = gateStatusDto.frontCamera;
            if (cameraStatusDto != null &&
                    cameraStatusDto.getIp().equals(carPictureFromRestDto.getIp_address())) {
                if (cameraStatusDto.snapshotEnabled != null) {
                    if (!cameraStatusDto.snapshotEnabled) {
                        return;
                    }
                    saveImageFromBase64ToPicture(carPictureFromRestDto);
                }
            }
            CameraStatusDto cameraStatusDto2 = gateStatusDto.backCamera;
            if (cameraStatusDto2 != null &&
                    cameraStatusDto2.getIp().equals(carPictureFromRestDto.getIp_address())) {
                if (cameraStatusDto2.snapshotEnabled != null) {
                    if (!cameraStatusDto2.snapshotEnabled) {
                        return;
                    }
                    saveImageFromBase64ToPicture(carPictureFromRestDto);
                }

            }
            CameraStatusDto cameraStatusDto3 = gateStatusDto.frontCamera2;
            if (cameraStatusDto3 != null &&
                    cameraStatusDto3.getIp().equals(carPictureFromRestDto.getIp_address())) {
                if (cameraStatusDto3.snapshotEnabled != null) {
                    if (!cameraStatusDto3.snapshotEnabled) {
                        return;
                    }
                    saveImageFromBase64ToPicture(carPictureFromRestDto);
                }
            }
        }
    }


    private void saveImageFromBase64ToPicture(CarPictureFromRestDto carPictureFromRestDto) throws IOException {
        try {
            byte[] decodedImageBytes = Base64.decodeBase64(carPictureFromRestDto.getCar_picture());
            Files.write(Paths.get(imagePath + "/" + carPictureFromRestDto.getIp_address().replace(".", "-") + ".jpeg"), decodedImageBytes);

        }
        catch (Exception e) {
            log.warning("Error of saveImageFromBase64ToPicture: " + e);
        }
    }

    public byte[] manualSnapShot(CameraStatusDto cameraStatusDto) {

        if(cameraStatusDto.snapshotUrl == null || cameraStatusDto.login ==  null || cameraStatusDto.password  == null){
            byte[] encodeToString = Base64.decodeBase64(noPicture);
            return encodeToString;
        }

        try {
            final int CONN_TIMEOUT = 10;
            String ip = cameraStatusDto.ip;
            HttpHost host = new HttpHost(ip, 8080, "http");
            CloseableHttpClient client = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider(cameraStatusDto.login, cameraStatusDto.password))
                    .useSystemProperties()
                    .build();
            HttpComponentsClientHttpRequestFactory requestFactory =
                    new HttpRequestFactoryDigestAuth(host, client);
            requestFactory.setConnectTimeout(CONN_TIMEOUT * 1000);
            requestFactory.setConnectionRequestTimeout(CONN_TIMEOUT * 1000);
            requestFactory.setReadTimeout(CONN_TIMEOUT * 1000);
            var restTemplate = new RestTemplate(requestFactory);
            val headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            StringBuilder address = new StringBuilder();
            address.append("http://");
            address.append(ip);
            address.append(cameraStatusDto.snapshotUrl);
            HttpEntity entity = new HttpEntity(headers);
            byte[] imageBytes = restTemplate.getForObject(address.toString(), byte[].class, entity);
            return imageBytes;
        }
        catch (Exception e) {
            log.warning("RestTemplate timeout for snapshot: " + e);
            byte[] encodeToString = Base64.decodeBase64(noPicture);
            return encodeToString ;
        }
    }

    public CredentialsProvider provider(String login, String password) {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials(login, password);
        provider.setCredentials(AuthScope.ANY, credentials);
        return provider;
    }

    public String encodeBase64StringWithSize(byte[] bytes)  {
        try {
            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(bytes))
                    .size(500, 500)
                    .outputFormat("JPEG")
                    .outputQuality(1)
                    .toOutputStream(resultStream);
            return StringUtils.newStringUtf8(Base64.encodeBase64(resultStream.toByteArray(), false));
        }
        catch (Exception e) {
            log.warning("Error of encodeBase64StringAndSize: " + e);
            return noPicture;
        }
    }
}
