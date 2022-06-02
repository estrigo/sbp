package kz.spt.app.rest;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Cars;
import kz.spt.lib.model.dto.CarEventDto;
import kz.spt.lib.service.CarsService;
import kz.spt.lib.service.CarEventService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(value = "/rest/cars")
public class CarsRestController {

    private CarEventService carEventService;
    private CarsService carsService;

    public CarsRestController(CarEventService carEventService, CarsService carsService){
        this.carEventService = carEventService;
        this.carsService = carsService;
    }

    @RequestMapping(value = "/event", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @Transactional
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addCarEvent(@Valid @RequestBody CarEventDto carEventDto) throws Exception{
        carEventDto.event_date_time = new Date(); // Не можем полагаться на время камеры
        carEventService.saveCarEvent(carEventDto);
    }

    @PostMapping
    public Page<Cars> list(@RequestBody PagingRequest pagingRequest) {
        return carsService.getCars(pagingRequest);
    }

    @RequestMapping(value = "/temp/event", method = RequestMethod.POST, consumes = "multipart/form-data")
    @Transactional
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addCarEvent(@RequestParam("upload") MultipartFile upload, @RequestParam("json") String json) throws Exception{
        carEventService.handleTempCarEvent(upload, json);
    }

    @GetMapping(value = "/search/plateNumber/{text}")
    public List<String> getCalibrationByIp(@PathVariable("text") String text) {
        return carsService.searchByPlateNumberContaining(text.toUpperCase());
    }

    @RequestMapping(value = "/rta/event", method = RequestMethod.POST, consumes = "multipart/form-data")
    @Transactional
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addCarEvent(@RequestParam("event_descriptor") String event_descriptor,
                            @RequestParam(value = "event_cropped_image_0", required = false) MultipartFile event_cropped_image_0,
                            @RequestParam("event_image_0") MultipartFile event_image_0,
                            @RequestParam(value = "event_timestamp", required = false) String event_timestamp) throws Exception{
        carEventService.handleRtaCarEvent(event_image_0, event_cropped_image_0, event_descriptor, event_timestamp);
    }

    @RequestMapping(value = "/carmen/event", method = RequestMethod.POST, consumes = "multipart/form-data")
    @Transactional
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void carmenEvent(@RequestParam("event_descriptor") String event_descriptor,
                            @RequestParam(value = "event_image_0", required = false) MultipartFile event_image_0,
                            @RequestParam(value = "event_timestamp", required = false) String event_timestamp) throws Exception {
        carEventService.handleLiveStreamEvent(event_image_0, event_descriptor, event_timestamp);
    }
}