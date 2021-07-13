package kz.spt.app.rest;

import kz.spt.app.entity.dto.CarEventDto;
import kz.spt.app.service.CarEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping(value = "/rest/cars")
public class CarsRestController {

    @Autowired
    private CarEventService carEventService;

    @RequestMapping(value = "/event", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @Transactional
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addCarEvent(@Valid @RequestBody CarEventDto carEventDto){
        carEventService.saveCarEvent(carEventDto);
    }

    @GetMapping(value = "/test", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String test() throws ParseException {

        SimpleDateFormat f = new SimpleDateFormat("dd:MM:yyyy hh:mm:ssZ");
        return f.format(new Date());
    }
}
