package kz.spt.app.rest;

import kz.spt.lib.service.CarImageService;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Log
@RestController
@RequestMapping(value = "/files")
public class ImageRestController {

    private CarImageService carImageService;

    public ImageRestController(CarImageService carImageService) {
        this.carImageService = carImageService;
    }

    @RequestMapping(value = "/pictures/{year}/{month}/{day}/{filename}", method = RequestMethod.GET)
    @ResponseBody
    public byte[] getImageContent(@PathVariable("year") String year, @PathVariable("month") String month, @PathVariable("day") String day, @PathVariable("filename") String filename) throws Exception {
        return carImageService.getByUrl("/" + year + "/" + month + "/" + day + "/" + filename);
    }

    @RequestMapping(value = "/pictures/{filename}", method = RequestMethod.GET)
    @ResponseBody
    public byte[] getSnapshot(@PathVariable("filename") String filename, @RequestParam("ver") String ver) throws Exception {
        return getImage(filename);
    }
    @RequestMapping(value = "/pic/v2/{filename}", method = RequestMethod.GET)
    @ResponseBody
    public  byte[] giveAnswerV2 (@PathVariable("filename") String filename, @RequestParam("ver") String ver) throws IOException {
        return getImage(filename);
    }

    private byte[] getImage (String filename) throws IOException {
        if (filename.substring(1)=="/") {
            return carImageService.getByUrl(filename);
        }
        return carImageService.getByUrl("/" + filename);
    }
}

