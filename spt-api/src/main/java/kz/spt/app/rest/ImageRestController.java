package kz.spt.app.rest;

import kz.spt.lib.service.CarImageService;
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

    @RequestMapping(value = "/pictures/{ip}/{year}/{month}/{day}/{filename}", method = RequestMethod.GET)
    @ResponseBody
    public byte[] getSnapshot(@PathVariable("ip") String ip,
                              @PathVariable("year") String year,
                              @PathVariable("month") String month,
                              @PathVariable("day") String day,
                              @PathVariable("filename") String filename,
                              @RequestParam("ver") String ver) throws Exception {
        return carImageService.getByUrl("/" + ip + "/" + year + "/" + month + "/" + day + "/" + filename);
    }
}
