package kz.spt.app.rest;

import kz.spt.lib.service.CarImageService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/rest/image")
public class ImageRestController {

    private CarImageService carImageService;

    public ImageRestController(CarImageService carImageService){
        this.carImageService = carImageService;
    }

    @RequestMapping(value = "/value/{eventId}", method = RequestMethod.GET)
    @ResponseBody
    public byte[] getImageContent(@PathVariable("eventId") Long eventId) throws Exception{
        return carImageService.getImage(eventId);
    }

    @RequestMapping(value = "/value/small/{eventId}", method = RequestMethod.GET)
    @ResponseBody
    public byte[] getSmallImageContent(@PathVariable("eventId") Long eventId) throws Exception{
        return carImageService.getSmallImage(eventId);
    }

    @RequestMapping(value = "/fix/small", method = RequestMethod.GET)
    @ResponseBody
    public void getSmallImageContent() throws Exception{
        carImageService.fixSmall();
    }
}
