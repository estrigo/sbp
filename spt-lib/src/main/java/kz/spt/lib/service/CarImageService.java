package kz.spt.lib.service;

import kz.spt.lib.model.dto.CameraStatusDto;
import kz.spt.lib.model.dto.CarPictureFromRestDto;
import org.apache.http.client.CredentialsProvider;

import java.io.IOException;
import java.util.Date;

public interface CarImageService {

    String saveImage(String base64, Date eventDate, String carNumber) throws IOException;

    void saveSnapshot(byte[] image, String carNumber) throws IOException;

    byte[] getByUrl(String url) throws IOException;

    void checkSnapshotEnabled(CarPictureFromRestDto carPictureFromRestDto) throws IOException;

    byte[] manualSnapShot(CameraStatusDto cameraStatusDto);

    CredentialsProvider provider(String login, String password);

    String encodeBase64StringWithSize(byte[] bytes);
}
