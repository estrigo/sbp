package kz.spt.app.rest;

import kz.spt.lib.service.CarImageService;
import kz.spt.lib.utils.StaticValues;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.*;

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

    @RequestMapping(value = "/pictures1/{filename}", method = RequestMethod.GET)
    @ResponseBody
    public byte[] getSnapshot(@PathVariable("filename") String filename, @RequestParam("ver") String ver) throws Exception {
        log.info(filename + " -------filename");
        if (filename.substring(1)=="/") {
            log.info("filename.substring(1)==/");
            return carImageService.getByUrl(filename);
        }
        log.info("without /");
        return carImageService.getByUrl("/" + filename);
    }
}
