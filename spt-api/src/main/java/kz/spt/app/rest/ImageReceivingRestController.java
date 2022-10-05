package kz.spt.app.rest;

import kz.spt.lib.model.dto.CarPictureFromRestDto;
import kz.spt.lib.service.CarImageService;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping(value = "/rest")
@Log
public class ImageReceivingRestController {
    private CarImageService carImageService;

    public ImageReceivingRestController(CarImageService carImageService) {
        this.carImageService = carImageService;
    }

    @RequestMapping(value = "/arm/picture", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    public void pictureReader (@RequestBody CarPictureFromRestDto carPictureFromRestDto) throws IOException {
        carImageService.checkSnapshotEnabled(carPictureFromRestDto);
    }
}
