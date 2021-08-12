package kz.spt.app.rest;

import kz.spt.app.service.CarImageService;
import org.springframework.web.bind.annotation.*;


import javax.websocket.server.PathParam;

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
}
