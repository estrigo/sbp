package kz.spt.lib.service;

import kz.spt.lib.model.dto.CarPictureFromRestDto;

import java.io.IOException;
import java.util.Date;

public interface CarImageService {

    String saveImage(String base64, Date eventDate, String carNumber) throws IOException;

    void saveSnapshot(byte[] image, String carNumber) throws IOException;

    byte[] getByUrl(String url) throws IOException;

    void checkSnapshotEnabled(CarPictureFromRestDto carPictureFromRestDto) throws IOException;
}
