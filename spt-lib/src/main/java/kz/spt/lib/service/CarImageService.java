package kz.spt.lib.service;

import java.io.IOException;
import java.util.Date;

public interface CarImageService {

    String saveImage(String base64, Date eventDate, String carNumber) throws IOException;

    byte[] getByUrl(String url) throws IOException;
}
